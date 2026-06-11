#include "INIFile.h"
#include <fstream>
#include <algorithm>
#include <regex>

namespace PNG2WAD {
namespace INI {

    INIFile::INIFile() {
        Clear();
    }

    INIFile::INIFile(const std::string& filePath) {
        LoadFromFile(filePath);
    }

    INIFile::~INIFile() {
        Clear();
    }

    void INIFile::Clear() {
        for (auto& pair : entries) {
            pair.second.clear();
        }
        entries.clear();
    }

    std::vector<std::string> INIFile::GetAllSections() {
        std::vector<std::string> keys;
        for (const auto& pair : entries) {
            keys.push_back(pair.first);
        }
        std::sort(keys.begin(), keys.end());
        return keys;
    }

    void INIFile::LoadFromFile(const std::string& filePath) {
        Clear();
        std::ifstream file(filePath);
        if (!file.is_open()) return;

        std::string dataString((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());
        ParseString(dataString);
    }

    void INIFile::LoadFromDataString(const std::string& dataString) {
        ParseString(dataString);
    }

    std::vector<std::string> INIFile::GetAllKeysInSection(const std::string& sectionKey, bool topLevelOnly) {
        if (entries.find(sectionKey) == entries.end()) return {};

        std::vector<std::string> keys;
        if (topLevelOnly) {
            for (const auto& pair : entries[sectionKey]) {
                std::string key = pair.first;
                size_t pos = key.find('.');
                if (pos != std::string::npos) {
                    key = key.substr(0, pos);
                }
                if (std::find(keys.begin(), keys.end(), key) == keys.end()) {
                    keys.push_back(key);
                }
            }
        } else {
            for (const auto& pair : entries[sectionKey]) {
                keys.push_back(pair.first);
            }
        }
        std::sort(keys.begin(), keys.end());
        return keys;
    }

    bool INIFile::SectionExists(const std::string& sectionKey) {
        return entries.find(sectionKey) != entries.end();
    }

    bool INIFile::ValueExists(const std::string& sectionKey, const std::string& keyID) {
        return entries.find(sectionKey) != entries.end() && entries[sectionKey].find(keyID) != entries[sectionKey].end();
    }

    void INIFile::RemoveSection(const std::string& sectionKey) {
        entries.erase(sectionKey);
    }

    void INIFile::RemoveKey(const std::string& sectionKey, const std::string& keyID) {
        if (entries.find(sectionKey) != entries.end()) {
            entries[sectionKey].erase(keyID);
        }
    }

    std::string INIFile::NormalizeKey(std::string key) {
        if (key.empty()) return "0";
        std::transform(key.begin(), key.end(), key.begin(), ::tolower);
        std::replace(key.begin(), key.end(), ' ', '-');
        key = std::regex_replace(key, std::regex("[^a-z0-9-\\.]"), "-");
        // Trim '-'
        key.erase(0, key.find_first_not_of('-'));
        if (!key.empty()) {
            key.erase(key.find_last_not_of('-') + 1);
        }
        while (key.find("--") != std::string::npos) {
            key.replace(key.find("--"), 2, "-");
        }
        return key;
    }

    std::string INIFile::GetRawValue(const std::string& sectionKey, const std::string& keyID) {
        std::string sKey = NormalizeKey(sectionKey);
        std::string kID = NormalizeKey(keyID);

        if (!ValueExists(sKey, kID)) return "";
        return entries[sKey][kID];
    }

    void INIFile::SetRawValue(const std::string& sectionKey, const std::string& keyID, const std::string& value) {
        std::string sKey = NormalizeKey(sectionKey);
        std::string kID = NormalizeKey(keyID);
        
        entries[sKey][kID] = value;
    }

    void INIFile::ParseString(const std::string& dataString) {
        Clear();

        std::string str = dataString;
        size_t pos = 0;
        while ((pos = str.find("\r\n", pos)) != std::string::npos) {
            str.replace(pos, 2, "\n");
            pos += 1;
        }

        std::stringstream ss(str);
        std::string line;
        std::string currentSection = "";
        bool inSection = false;

        while (std::getline(ss, line, '\n')) {
            // Trim
            line.erase(0, line.find_first_not_of(" \t\r\n"));
            if (!line.empty()) {
                line.erase(line.find_last_not_of(" \t\r\n") + 1);
            }

            if (line.empty() || line.find(";") == 0) continue;

            if (line.front() == '[' && line.back() == ']') {
                currentSection = line.substr(1, line.length() - 2);
                currentSection.erase(0, currentSection.find_first_not_of(" \t\r\n"));
                if (!currentSection.empty()) {
                    currentSection.erase(currentSection.find_last_not_of(" \t\r\n") + 1);
                }
                std::transform(currentSection.begin(), currentSection.end(), currentSection.begin(), ::tolower);
                if (currentSection.empty()) {
                    inSection = false;
                } else {
                    inSection = true;
                }
                continue;
            }

            if (!inSection) continue;

            size_t eqPos = line.find('=');
            if (eqPos == std::string::npos) continue;

            std::string key = line.substr(0, eqPos);
            std::string value = line.substr(eqPos + 1);

            key.erase(0, key.find_first_not_of(" \t\r\n"));
            if (!key.empty()) {
                key.erase(key.find_last_not_of(" \t\r\n") + 1);
            }
            std::transform(key.begin(), key.end(), key.begin(), ::tolower);

            if (key.empty()) continue;

            SetRawValue(currentSection, key, value);
        }
    }

    std::vector<std::string> INIFile::GetValueArrayNormalizedStrings(const std::string& sectionKey, const std::string& keyID, bool distinct) {
        std::string valuesString = GetRawValue(sectionKey, keyID);
        if (valuesString.empty()) return {};

        std::vector<std::string> result;
        std::stringstream ss(valuesString);
        std::string item;
        while (std::getline(ss, item, ',')) {
            item.erase(0, item.find_first_not_of(" \t\r\n"));
            if (!item.empty()) {
                item.erase(item.find_last_not_of(" \t\r\n") + 1);
            }
            if (!item.empty()) {
                std::transform(item.begin(), item.end(), item.begin(), ::tolower);
                if (distinct) {
                    if (std::find(result.begin(), result.end(), item) == result.end()) {
                        result.push_back(item);
                    }
                } else {
                    result.push_back(item);
                }
            }
        }
        return result;
    }

    void INIFile::SaveToFile(const std::string& filePath) {
        std::string dataString = "";
        bool firstSection = true;

        for (const std::string& entryKey : GetAllSections()) {
            if (firstSection) firstSection = false;
            else dataString += "\r\n";
            dataString += "[" + entryKey + "]\r\n";

            for (const std::string& keyID : GetAllKeysInSection(entryKey)) {
                dataString += keyID + "=" + GetRawValue(entryKey, keyID) + "\r\n";
            }
        }

        std::ofstream file(filePath);
        if (file.is_open()) {
            file << dataString;
        }
    }

}
}