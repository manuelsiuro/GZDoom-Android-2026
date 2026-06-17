#pragma once
//
// TextureReader: decode real Doom textures/flats/sprites from one or more WADs
// (an IWAD plus optional override PWADs) into RGBA bitmaps for the editor's
// texture browser. Reuses WadFile for lump access. Knows the classic on-disk
// formats: PLAYPAL (palette), PNAMES + TEXTURE1/TEXTURE2 (composite walls),
// raw 64x64 flats, and the Doom column "picture" format (patches + sprites).
//
#include <string>
#include <vector>
#include <map>
#include <memory>
#include <cstdint>

class WadFile;
struct WadLump;

namespace png2wad {

// Decoded RGBA image. argb is row-major, 0xAARRGGBB per pixel (matches the int
// layout Android's Bitmap.createBitmap(int[], w, h, ARGB_8888) expects).
struct DecodedImage {
    int width = 0;
    int height = 0;
    std::vector<uint32_t> argb;
    bool valid() const { return width > 0 && height > 0 && argb.size() == static_cast<size_t>(width) * height; }
};

class TextureReader {
public:
    TextureReader();
    ~TextureReader();

    // Load the IWAD then each extra WAD. Later WADs override earlier ones for
    // same-named lumps/patches/flats (standard Doom load order). Returns true
    // only if a palette was found (without it nothing can be decoded).
    bool Open(const std::string& iwadPath, const std::vector<std::string>& extraWadPaths);

    const std::vector<std::string>& WallNames() const { return wallNames_; }
    const std::vector<std::string>& FlatNames() const { return flatNames_; }

    // Resolve any name: composite texture first, then flat, then a raw patch /
    // sprite lump. Returns an invalid image if the name can't be decoded.
    DecodedImage GetImage(const std::string& name);

private:
    struct PatchDef { int originX; int originY; int patchIndex; };
    struct TextureDef { std::string name; int width; int height; std::vector<PatchDef> patches; };

    // WADs are heap-owned so lump pointers stay stable as the vector grows.
    std::vector<std::unique_ptr<WadFile>> wads_;

    uint8_t palette_[256][3];
    bool hasPalette_ = false;

    std::vector<std::string> pnames_;                 // patch lump names (index = patch id)
    std::vector<TextureDef> textures_;                // composite wall textures
    std::map<std::string, size_t> textureIndex_;      // upper name -> index in textures_
    std::vector<std::string> wallNames_;              // ordered wall names for the UI
    std::vector<std::string> flatNames_;              // ordered flat names for the UI
    std::map<std::string, bool> flatSet_;             // upper name -> is a flat

    // Searches WADs in reverse so override PWADs win over the IWAD.
    const WadLump* FindLump(const std::string& name) const;

    void LoadPalette();
    void LoadPnames();
    void LoadTextureLump(const std::string& lumpName);
    void LoadFlats();

    DecodedImage DecodeFlat(const std::vector<uint8_t>& bytes) const;
    DecodedImage DecodePatch(const std::vector<uint8_t>& bytes) const;
    DecodedImage DecodeComposite(const TextureDef& def);
};

} // namespace png2wad
