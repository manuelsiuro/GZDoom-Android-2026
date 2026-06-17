#include "TextureReader.h"
#include "WadFile.h"
#include "Log.h"
#include <algorithm>
#include <cctype>

namespace png2wad {

namespace {
// Little-endian readers (WAD data is LE; read explicitly for portability/safety).
inline int16_t ReadI16(const std::vector<uint8_t>& b, size_t off) {
    return static_cast<int16_t>(b[off] | (b[off + 1] << 8));
}
inline uint16_t ReadU16(const std::vector<uint8_t>& b, size_t off) {
    return static_cast<uint16_t>(b[off] | (b[off + 1] << 8));
}
inline int32_t ReadI32(const std::vector<uint8_t>& b, size_t off) {
    return static_cast<int32_t>(b[off] | (b[off + 1] << 8) | (b[off + 2] << 16) | (b[off + 3] << 24));
}
inline uint32_t ReadU32(const std::vector<uint8_t>& b, size_t off) {
    return static_cast<uint32_t>(b[off] | (b[off + 1] << 8) | (b[off + 2] << 16) | (b[off + 3] << 24));
}
std::string ReadName8(const std::vector<uint8_t>& b, size_t off) {
    std::string s;
    for (size_t i = 0; i < 8 && off + i < b.size(); i++) {
        char c = static_cast<char>(b[off + i]);
        if (c == '\0') break;
        s += static_cast<char>(std::toupper(static_cast<unsigned char>(c)));
    }
    return s;
}
bool EndsWith(const std::string& s, const char* suffix) {
    size_t n = std::char_traits<char>::length(suffix);
    return s.size() >= n && s.compare(s.size() - n, n, suffix) == 0;
}
} // namespace

TextureReader::TextureReader() {
    std::fill(&palette_[0][0], &palette_[0][0] + 256 * 3, 0);
}
TextureReader::~TextureReader() = default;

bool TextureReader::Open(const std::string& iwadPath, const std::vector<std::string>& extraWadPaths) {
    wads_.push_back(std::make_unique<WadFile>(iwadPath));
    for (const auto& p : extraWadPaths) {
        wads_.push_back(std::make_unique<WadFile>(p));
    }

    LoadPalette();
    if (!hasPalette_) {
        LOGE("TextureReader: no PLAYPAL found in '%s'; cannot decode textures.", iwadPath.c_str());
        return false;
    }
    LoadPnames();
    LoadTextureLump("TEXTURE1");
    LoadTextureLump("TEXTURE2");
    LoadFlats();
    LOGI("TextureReader: %zu wall texture(s), %zu flat(s), %zu patch name(s).",
         wallNames_.size(), flatNames_.size(), pnames_.size());
    return true;
}

const WadLump* TextureReader::FindLump(const std::string& name) const {
    for (auto it = wads_.rbegin(); it != wads_.rend(); ++it) {
        const WadLump* l = (*it)->GetLumpByName(name);
        if (l) return l;
    }
    return nullptr;
}

void TextureReader::LoadPalette() {
    const WadLump* l = FindLump("PLAYPAL");
    if (!l || l->Bytes.size() < 768) return;
    for (int i = 0; i < 256; i++) {
        palette_[i][0] = l->Bytes[i * 3 + 0];
        palette_[i][1] = l->Bytes[i * 3 + 1];
        palette_[i][2] = l->Bytes[i * 3 + 2];
    }
    hasPalette_ = true;
}

void TextureReader::LoadPnames() {
    const WadLump* l = FindLump("PNAMES");
    if (!l || l->Bytes.size() < 4) return;
    const auto& b = l->Bytes;
    int count = ReadI32(b, 0);
    size_t off = 4;
    for (int i = 0; i < count && off + 8 <= b.size(); i++, off += 8) {
        pnames_.push_back(ReadName8(b, off));
    }
}

void TextureReader::LoadTextureLump(const std::string& lumpName) {
    const WadLump* l = FindLump(lumpName);
    if (!l || l->Bytes.size() < 4) return;
    const auto& b = l->Bytes;
    int numTex = ReadI32(b, 0);
    for (int t = 0; t < numTex; t++) {
        size_t ofsPos = 4 + 4 * static_cast<size_t>(t);
        if (ofsPos + 4 > b.size()) break;
        size_t ofs = static_cast<size_t>(ReadI32(b, ofsPos));
        if (ofs + 22 > b.size()) continue;

        TextureDef def;
        def.name = ReadName8(b, ofs);              // [ofs+0 .. +7] name
        def.width = ReadU16(b, ofs + 12);          // [ofs+8..+11] masked (skip)
        def.height = ReadU16(b, ofs + 14);
        int patchCount = ReadU16(b, ofs + 20);     // [ofs+16..+19] columndir (skip)

        size_t p = ofs + 22;
        for (int pc = 0; pc < patchCount; pc++) {
            if (p + 10 > b.size()) break;
            PatchDef pd;
            pd.originX = ReadI16(b, p);
            pd.originY = ReadI16(b, p + 2);
            pd.patchIndex = ReadU16(b, p + 4);     // [p+6] stepdir, [p+8] colormap (skip)
            def.patches.push_back(pd);
            p += 10;
        }
        if (def.name.empty()) continue;

        auto found = textureIndex_.find(def.name);
        if (found == textureIndex_.end()) {
            textureIndex_[def.name] = textures_.size();
            textures_.push_back(std::move(def));
            wallNames_.push_back(textures_.back().name);
        } else {
            textures_[found->second] = std::move(def); // PWAD override
        }
    }
}

void TextureReader::LoadFlats() {
    for (auto& wad : wads_) {
        std::vector<std::string> names = wad->GetLumpNames();
        bool inFlats = false;
        for (size_t i = 0; i < names.size(); i++) {
            const std::string& n = names[i];
            if (n == "F_START" || n == "FF_START") { inFlats = true; continue; }
            if (n == "F_END" || n == "FF_END") { inFlats = false; continue; }
            if (!inFlats) continue;
            if (EndsWith(n, "_START") || EndsWith(n, "_END")) continue; // F1_START etc.
            const WadLump* l = wad->GetLumpByIndex(static_cast<int>(i));
            if (!l || l->Bytes.size() != 4096) continue;               // 64x64 raw flat
            if (flatSet_.find(n) == flatSet_.end()) {
                flatSet_[n] = true;
                flatNames_.push_back(n);
            }
        }
    }
}

DecodedImage TextureReader::DecodeFlat(const std::vector<uint8_t>& bytes) const {
    DecodedImage img;
    if (bytes.size() < 4096) return img;
    img.width = 64;
    img.height = 64;
    img.argb.resize(64 * 64);
    for (int i = 0; i < 64 * 64; i++) {
        uint8_t idx = bytes[i];
        img.argb[i] = 0xFF000000u |
                      (static_cast<uint32_t>(palette_[idx][0]) << 16) |
                      (static_cast<uint32_t>(palette_[idx][1]) << 8) |
                      static_cast<uint32_t>(palette_[idx][2]);
    }
    return img;
}

DecodedImage TextureReader::DecodePatch(const std::vector<uint8_t>& bytes) const {
    DecodedImage img;
    if (bytes.size() < 8) return img;
    int w = ReadU16(bytes, 0);
    int h = ReadU16(bytes, 2);
    // [4]=leftoffset, [6]=topoffset — irrelevant for a thumbnail.
    if (w <= 0 || h <= 0 || w > 4096 || h > 4096) return img;
    if (bytes.size() < 8 + 4u * w) return img;

    img.width = w;
    img.height = h;
    img.argb.assign(static_cast<size_t>(w) * h, 0); // transparent

    for (int col = 0; col < w; col++) {
        size_t p = ReadU32(bytes, 8 + 4u * col);
        while (p < bytes.size()) {
            uint8_t topDelta = bytes[p++];
            if (topDelta == 0xFF) break;
            if (p >= bytes.size()) break;
            uint8_t length = bytes[p++];
            if (p >= bytes.size()) break;
            p++; // unused pre-pad byte
            for (int i = 0; i < length && p < bytes.size(); i++) {
                uint8_t idx = bytes[p++];
                int y = topDelta + i;
                if (y >= 0 && y < h) {
                    img.argb[static_cast<size_t>(y) * w + col] =
                        0xFF000000u |
                        (static_cast<uint32_t>(palette_[idx][0]) << 16) |
                        (static_cast<uint32_t>(palette_[idx][1]) << 8) |
                        static_cast<uint32_t>(palette_[idx][2]);
                }
            }
            if (p < bytes.size()) p++; // unused post-pad byte
        }
    }
    return img;
}

DecodedImage TextureReader::DecodeComposite(const TextureDef& def) {
    DecodedImage img;
    if (def.width <= 0 || def.height <= 0 || def.width > 4096 || def.height > 4096) return img;
    img.width = def.width;
    img.height = def.height;
    img.argb.assign(static_cast<size_t>(def.width) * def.height, 0xFF000000u); // opaque black base

    for (const auto& pd : def.patches) {
        if (pd.patchIndex < 0 || pd.patchIndex >= static_cast<int>(pnames_.size())) continue;
        const WadLump* pl = FindLump(pnames_[pd.patchIndex]);
        if (!pl) continue;
        DecodedImage patch = DecodePatch(pl->Bytes);
        if (!patch.valid()) continue;
        for (int py = 0; py < patch.height; py++) {
            int dy = pd.originY + py;
            if (dy < 0 || dy >= def.height) continue;
            for (int px = 0; px < patch.width; px++) {
                int dx = pd.originX + px;
                if (dx < 0 || dx >= def.width) continue;
                uint32_t c = patch.argb[static_cast<size_t>(py) * patch.width + px];
                if ((c >> 24) != 0) {
                    img.argb[static_cast<size_t>(dy) * def.width + dx] = c;
                }
            }
        }
    }
    return img;
}

DecodedImage TextureReader::GetImage(const std::string& rawName) {
    std::string name = rawName;
    std::transform(name.begin(), name.end(), name.begin(),
                   [](unsigned char c) { return std::toupper(c); });

    auto tex = textureIndex_.find(name);
    if (tex != textureIndex_.end()) {
        return DecodeComposite(textures_[tex->second]);
    }
    if (flatSet_.find(name) != flatSet_.end()) {
        const WadLump* l = FindLump(name);
        if (l) return DecodeFlat(l->Bytes);
    }
    // Fall back to a raw patch / sprite lump (used for thing sprite thumbnails).
    const WadLump* l = FindLump(name);
    if (l) {
        DecodedImage img = DecodePatch(l->Bytes);
        if (img.valid()) return img;
        if (l->Bytes.size() == 4096) return DecodeFlat(l->Bytes);
    }
    return DecodedImage();
}

} // namespace png2wad
