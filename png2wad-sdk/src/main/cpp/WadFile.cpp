#include "WadFile.h"
#include "Log.h"
#include <fstream>
#include <algorithm>
#include <cstring>
#include <regex>

WadLump::WadLump(const std::string& name, const std::vector<uint8_t>& bytes) : Bytes(bytes) {
    std::string safeName = name;
    if (safeName.empty()) safeName = "NULL";
    
    std::transform(safeName.begin(), safeName.end(), safeName.begin(), ::toupper);
    safeName = std::regex_replace(safeName, std::regex("[^A-Z0-9_]"), "");
    
    if (safeName.length() > WadFile::MAX_LUMP_NAME_LENGTH) {
        safeName = safeName.substr(0, WadFile::MAX_LUMP_NAME_LENGTH);
    }
    Name = safeName;
}

WadFile::WadFile() {}

WadFile::WadFile(const std::string& filePath) {
    std::ifstream fs(filePath, std::ios::binary);
    if (!fs) return;

    std::vector<uint8_t> buffer4(4);
    std::vector<uint8_t> buffer8(8);

    fs.read(reinterpret_cast<char*>(buffer4.data()), 4);
    if (!fs) return;
    
    std::string typeStr = GetStringFromBytes(buffer4);
    if (typeStr == "IWAD") {
        IWAD = true;
    } else if (typeStr == "PWAD") {
        IWAD = false;
    } else {
        return; // invalid format
    }

    fs.read(reinterpret_cast<char*>(buffer4.data()), 4);
    int lumpCount = *reinterpret_cast<int*>(buffer4.data());
    if (lumpCount <= 0) return;

    fs.read(reinterpret_cast<char*>(buffer4.data()), 4);
    int directoryOffset = *reinterpret_cast<int*>(buffer4.data());
    if (directoryOffset < 12) return;

    for (int i = 0; i < lumpCount; i++) {
        fs.seekg(directoryOffset + 16 * i, std::ios::beg);

        fs.read(reinterpret_cast<char*>(buffer4.data()), 4);
        int lumpOffset = *reinterpret_cast<int*>(buffer4.data());
        
        fs.read(reinterpret_cast<char*>(buffer4.data()), 4);
        int lumpSize = *reinterpret_cast<int*>(buffer4.data());
        
        fs.read(reinterpret_cast<char*>(buffer8.data()), 8);
        std::string lumpName = GetStringFromBytes(buffer8);

        std::vector<uint8_t> lumpbytes(lumpSize);
        fs.seekg(lumpOffset, std::ios::beg);
        fs.read(reinterpret_cast<char*>(lumpbytes.data()), lumpSize);
        
        Lumps.emplace_back(lumpName, lumpbytes);
    }
}

int WadFile::GetLumpCount() const {
    return Lumps.size();
}

void WadFile::ClearLumps() {
    Lumps.clear();
}

const WadLump* WadFile::GetLumpByName(const std::string& lumpName) const {
    int index = GetFirstIndexByLumpName(lumpName);
    if (index == -1) return nullptr;
    return &Lumps[index];
}

const WadLump* WadFile::GetLumpByIndex(int index) const {
    if (index < 0 || index >= Lumps.size()) return nullptr;
    return &Lumps[index];
}

std::vector<int> WadFile::GetIndicesByLumpName(const std::string& lumpName) const {
    if (lumpName.empty()) return {};
    std::string upperName = lumpName;
    std::transform(upperName.begin(), upperName.end(), upperName.begin(), ::toupper);
    
    std::vector<int> indices;
    for (size_t i = 0; i < Lumps.size(); i++) {
        if (Lumps[i].Name == upperName) {
            indices.push_back(i);
        }
    }
    return indices;
}

int WadFile::GetFirstIndexByLumpName(const std::string& lumpName) const {
    std::vector<int> indices = GetIndicesByLumpName(lumpName);
    return indices.empty() ? -1 : indices[0];
}

bool WadFile::RemoveLump(const std::string& lumpName, bool removeAll) {
    if (removeAll) {
        int lumpIndex = GetFirstIndexByLumpName(lumpName);
        if (lumpIndex < 0) return false;

        while (lumpIndex >= 0) {
            Lumps.erase(Lumps.begin() + lumpIndex);
            lumpIndex = GetFirstIndexByLumpName(lumpName);
        }
        return true;
    }
    return RemoveLump(GetFirstIndexByLumpName(lumpName));
}

bool WadFile::RemoveLump(int index) {
    if (index < 0 || index >= Lumps.size()) return false;
    Lumps.erase(Lumps.begin() + index);
    return true;
}

std::vector<std::string> WadFile::GetLumpNames() const {
    std::vector<std::string> names;
    names.reserve(Lumps.size());
    for (const auto& lump : Lumps) {
        names.push_back(lump.Name);
    }
    return names;
}

void WadFile::SaveToFile(const std::string& wadFilePath) const {
    int directoryOffset = 12;
    for (const auto& lump : Lumps) {
        directoryOffset += lump.Bytes.size();
    }

    std::vector<uint8_t> headerBytes;
    std::string iwadStr = IWAD ? "IWAD" : "PWAD";
    headerBytes.insert(headerBytes.end(), iwadStr.begin(), iwadStr.end());
    
    int lumpCount = Lumps.size();
    auto countPtr = reinterpret_cast<const uint8_t*>(&lumpCount);
    headerBytes.insert(headerBytes.end(), countPtr, countPtr + 4);
    
    auto offsetPtr = reinterpret_cast<const uint8_t*>(&directoryOffset);
    headerBytes.insert(headerBytes.end(), offsetPtr, offsetPtr + 4);

    std::vector<uint8_t> directoryBytes;
    int byteOffset = 12;
    for (const auto& l : Lumps) {
        auto bOffsetPtr = reinterpret_cast<const uint8_t*>(&byteOffset);
        directoryBytes.insert(directoryBytes.end(), bOffsetPtr, bOffsetPtr + 4);
        
        int lumpSize = l.Bytes.size();
        auto lSizePtr = reinterpret_cast<const uint8_t*>(&lumpSize);
        directoryBytes.insert(directoryBytes.end(), lSizePtr, lSizePtr + 4);
        
        std::vector<uint8_t> nameBytes = GetBytesFromString(l.Name);
        directoryBytes.insert(directoryBytes.end(), nameBytes.begin(), nameBytes.end());
        
        byteOffset += lumpSize;
    }

    std::ofstream fs(wadFilePath, std::ios::binary);
    if (!fs) {
        LOGE("SaveToFile: could not open '%s' for writing.", wadFilePath.c_str());
        return;
    }

    fs.write(reinterpret_cast<const char*>(headerBytes.data()), headerBytes.size());
    for (const auto& l : Lumps) {
        if (!l.Bytes.empty()) {
            fs.write(reinterpret_cast<const char*>(l.Bytes.data()), l.Bytes.size());
        }
    }
    fs.write(reinterpret_cast<const char*>(directoryBytes.data()), directoryBytes.size());

    long totalSize = static_cast<long>(headerBytes.size()) + directoryBytes.size();
    for (const auto& l : Lumps) totalSize += l.Bytes.size();

    LOGI("WAD header: %s, lumps=%d, dirOffset=%d, totalSize=%ld bytes",
         IWAD ? "IWAD" : "PWAD", lumpCount, directoryOffset, totalSize);
    int logOffset = 12;
    for (const auto& l : Lumps) {
        LOGI("  lump '%-8s' offset=%d size=%zu", l.Name.c_str(), logOffset, l.Bytes.size());
        logOffset += l.Bytes.size();
    }
}

std::vector<uint8_t> WadFile::GetBytesFromString(const std::string& text, int length) {
    if (length <= 0) return {};
    std::vector<uint8_t> bytes(length, 0);
    if (text.empty()) return bytes;
    
    for (size_t i = 0; i < std::min(text.length(), static_cast<size_t>(length)); i++) {
        bytes[i] = text[i];
    }
    return bytes;
}

std::string WadFile::GetStringFromBytes(const std::vector<uint8_t>& bytes) {
    if (bytes.empty()) return "";
    std::string str;
    for (uint8_t b : bytes) {
        if (b == '\0') break;
        str += b;
    }
    std::transform(str.begin(), str.end(), str.begin(), ::toupper);
    return str;
}

void WadFile::AddLump(const std::string& lumpName, const std::vector<uint8_t>& bytes) {
    Lumps.emplace_back(lumpName, bytes);
}

void WadFile::Dispose() {
    ClearLumps();
}
