#pragma once
#include <string>
#include <vector>
#include <cstdint>

struct WadLump {
    std::string Name;
    std::vector<uint8_t> Bytes;
    WadLump(const std::string& name, const std::vector<uint8_t>& bytes);
};

class WadFile {
public:
    static const int MAX_LUMP_NAME_LENGTH = 8;
    bool IWAD = false;

    WadFile();
    WadFile(const std::string& filePath);
    ~WadFile() = default;

    int GetLumpCount() const;
    void ClearLumps();
    
    const WadLump* GetLumpByName(const std::string& lumpName) const;
    const WadLump* GetLumpByIndex(int index) const;

    std::vector<int> GetIndicesByLumpName(const std::string& lumpName) const;
    int GetFirstIndexByLumpName(const std::string& lumpName) const;
    
    bool RemoveLump(const std::string& lumpName, bool removeAll = false);
    bool RemoveLump(int index);

    std::vector<std::string> GetLumpNames() const;

    void SaveToFile(const std::string& wadFilePath) const;
    static std::vector<uint8_t> GetBytesFromString(const std::string& text, int length = MAX_LUMP_NAME_LENGTH);
    static std::string GetStringFromBytes(const std::vector<uint8_t>& bytes);

    void AddLump(const std::string& lumpName, const std::vector<uint8_t>& bytes);
    void Dispose();

private:
    std::vector<WadLump> Lumps;
};
