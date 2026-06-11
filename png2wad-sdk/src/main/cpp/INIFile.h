#pragma once

#include <string>
#include <map>
#include <vector>
#include <algorithm>
#include <sstream>

namespace PNG2WAD {
namespace INI {

    class INIFile {
    protected:
        struct CaseInsensitiveCompare {
            bool operator()(const std::string& a, const std::string& b) const {
                std::string a_lower = a;
                std::string b_lower = b;
                std::transform(a_lower.begin(), a_lower.end(), a_lower.begin(), ::tolower);
                std::transform(b_lower.begin(), b_lower.end(), b_lower.begin(), ::tolower);
                return a_lower < b_lower;
            }
        };

        std::map<std::string, std::map<std::string, std::string, CaseInsensitiveCompare>, CaseInsensitiveCompare> entries;

        static std::string NormalizeKey(std::string key);
        void ParseString(const std::string& dataString);

    public:
        INIFile();
        INIFile(const std::string& filePath);
        virtual ~INIFile();

        void Clear();
        std::vector<std::string> GetAllSections();
        void LoadFromFile(const std::string& filePath);
        void LoadFromDataString(const std::string& dataString);
        std::vector<std::string> GetAllKeysInSection(const std::string& sectionKey, bool topLevelOnly = false);
        bool SectionExists(const std::string& sectionKey);
        bool ValueExists(const std::string& sectionKey, const std::string& keyID);
        void RemoveSection(const std::string& sectionKey);
        void RemoveKey(const std::string& sectionKey, const std::string& keyID);

        std::string GetRawValue(const std::string& sectionKey, const std::string& keyID);
        void SetRawValue(const std::string& sectionKey, const std::string& keyID, const std::string& value);

        template<typename T>
        T ConvertFromString(const std::string& valueString) {
            if constexpr (std::is_same_v<T, bool>) {
                std::string lower = valueString;
                std::transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
                return lower == "true" || lower == "1";
            } else if constexpr (std::is_same_v<T, std::string>) {
                return valueString;
            } else {
                T value = T();
                std::stringstream ss(valueString);
                ss >> value;
                return value;
            }
        }

        template<typename T>
        std::string ConvertToString(const T& value) {
            if constexpr (std::is_same_v<T, bool>) {
                return value ? "true" : "false";
            } else if constexpr (std::is_same_v<T, std::string>) {
                return value;
            } else {
                std::stringstream ss;
                ss << value;
                return ss.str();
            }
        }

        template<typename T>
        T GetValue(const std::string& sectionKey, const std::string& keyID, T defaultValue = T()) {
            if (!ValueExists(sectionKey, keyID)) return defaultValue;
            return ConvertFromString<T>(GetRawValue(sectionKey, keyID));
        }

        template<typename ElementType>
        char GetArraySeparator() {
            if constexpr (std::is_same_v<ElementType, std::string>) {
                return ';';
            }
            return ',';
        }

        template<typename T>
        std::vector<T> GetValueArray(const std::string& sectionKey, const std::string& keyID, const std::vector<T>& defaultValues = {}) {
            if (!ValueExists(sectionKey, keyID)) return defaultValues;

            std::string rawValue = GetRawValue(sectionKey, keyID);
            char sep = GetArraySeparator<T>();
            std::vector<T> result;
            std::stringstream ss(rawValue);
            std::string item;
            while (std::getline(ss, item, sep)) {
                // Trim
                item.erase(0, item.find_first_not_of(" \t\r\n"));
                item.erase(item.find_last_not_of(" \t\r\n") + 1);
                if (!item.empty()) {
                    result.push_back(ConvertFromString<T>(item));
                }
            }
            return result;
        }

        template<typename T>
        std::map<std::string, std::vector<T>, CaseInsensitiveCompare> GetValueStringArrayDictionary(const std::string& sectionKey, const std::string& keyID) {
            std::map<std::string, std::vector<T>, CaseInsensitiveCompare> dict;
            dict[""] = GetValueArray<T>(sectionKey, keyID);

            std::string prefix = keyID + ".";
            std::transform(prefix.begin(), prefix.end(), prefix.begin(), ::tolower);

            std::vector<std::string> subKeys;
            for (const std::string& subKey : GetAllKeysInSection(sectionKey)) {
                std::string lowerSubKey = subKey;
                std::transform(lowerSubKey.begin(), lowerSubKey.end(), lowerSubKey.begin(), ::tolower);
                if (lowerSubKey.find(prefix) == 0 && lowerSubKey.length() > prefix.length()) {
                    size_t dotPos = subKey.find('.', keyID.length() + 1);
                    std::string part = subKey.substr(keyID.length() + 1, dotPos - (keyID.length() + 1));
                    std::transform(part.begin(), part.end(), part.begin(), ::tolower);
                    if (std::find(subKeys.begin(), subKeys.end(), part) == subKeys.end()) {
                        subKeys.push_back(part);
                    }
                }
            }

            for (const std::string& subKey : subKeys) {
                if (dict.find(subKey) == dict.end()) {
                    dict[subKey] = GetValueArray<T>(sectionKey, keyID + "." + subKey);
                }
            }
            return dict;
        }

        template<typename T>
        std::map<std::string, T, CaseInsensitiveCompare> GetValueStringDictionary(const std::string& sectionKey, const std::string& keyID) {
            std::map<std::string, T, CaseInsensitiveCompare> dict;
            dict[""] = GetValue<T>(sectionKey, keyID);

            std::string prefix = keyID + ".";
            std::transform(prefix.begin(), prefix.end(), prefix.begin(), ::tolower);

            std::vector<std::string> subKeys;
            for (const std::string& subKey : GetAllKeysInSection(sectionKey)) {
                std::string lowerSubKey = subKey;
                std::transform(lowerSubKey.begin(), lowerSubKey.end(), lowerSubKey.begin(), ::tolower);
                if (lowerSubKey.find(prefix) == 0 && lowerSubKey.length() > prefix.length()) {
                    size_t dotPos = subKey.find('.', keyID.length() + 1);
                    std::string part = subKey.substr(keyID.length() + 1, dotPos - (keyID.length() + 1));
                    std::transform(part.begin(), part.end(), part.begin(), ::tolower);
                    if (std::find(subKeys.begin(), subKeys.end(), part) == subKeys.end()) {
                        subKeys.push_back(part);
                    }
                }
            }

            for (const std::string& subKey : subKeys) {
                if (dict.find(subKey) == dict.end()) {
                    dict[subKey] = GetValue<T>(sectionKey, keyID + "." + subKey);
                }
            }
            return dict;
        }

        template<typename T, typename EnumType>
        std::map<EnumType, T> GetValueEnumDictionary(const std::string& sectionKey, const std::string& keyID) {
            std::map<std::string, T, CaseInsensitiveCompare> stringDict = GetValueStringDictionary<T>(sectionKey, keyID);
            std::map<EnumType, T> dict;
            for (int i = 0; i < static_cast<int>(EnumType::Count); i++) {
                EnumType eKey = static_cast<EnumType>(i);
                std::string sKey = ToString(eKey); // assumes ToString is implemented in the caller namespace
                if (stringDict.find(sKey) != stringDict.end()) {
                    dict[eKey] = stringDict[sKey];
                }
            }
            return dict;
        }

        template<typename T, typename TIndex>
        std::vector<T> GetValueArrayEnumIndex(const std::string& sectionKey, const std::string& keyID) {
            std::vector<T> values(static_cast<int>(TIndex::Count));
            for (int i = 0; i < static_cast<int>(TIndex::Count); i++) {
                std::string fullKey = keyID + "." + std::to_string(i); // closest approx to enum int cast
                if (!ValueExists(sectionKey, fullKey)) {
                    values[i] = T();
                    continue;
                }
                values[i] = GetValue<T>(sectionKey, keyID); // Logic from C#: values[i] = GetValue<T>(sectionKey, keyID);
            }
            return values;
        }

        std::vector<std::string> GetValueArrayNormalizedStrings(const std::string& sectionKey, const std::string& keyID, bool distinct = false);

        template<typename T, typename TIndex>
        std::vector<std::vector<T>> GetValueArrayArrayEnumIndex(const std::string& sectionKey, const std::string& keyID) {
            std::vector<std::vector<T>> values(static_cast<int>(TIndex::Count));
            for (int i = 0; i < static_cast<int>(TIndex::Count); i++) {
                std::string fullKey = keyID + "." + std::to_string(i);
                if (!ValueExists(sectionKey, fullKey)) {
                    values[i] = std::vector<T>();
                    continue;
                }
                values[i] = GetValueArray<T>(sectionKey, fullKey);
            }
            return values;
        }

        template<typename T>
        std::vector<T> GetValueList(const std::string& section, const std::string& key, const std::vector<T>& defaultValues = {}) {
            return GetValueArray<T>(section, key, defaultValues);
        }

        template<typename T>
        T GetValueAsFlags(const std::string& section, const std::string& key) {
            std::vector<T> valueArray = GetValueArray<T>(section, key);
            int valueFlags = 0;
            for (T value : valueArray) {
                valueFlags |= static_cast<int>(value);
            }
            return static_cast<T>(valueFlags);
        }

        template<typename TKey, typename TValue>
        std::map<TKey, TValue> GetValueDictionary(const std::string& section, const std::string& key, const std::map<TKey, TValue>& defaultPairs = {}) {
            if (!ValueExists(section, key)) {
                return defaultPairs;
            }
            std::map<TKey, TValue> dict;
            for (int i = 0; i < static_cast<int>(TKey::Count); i++) {
                TKey dictKey = static_cast<TKey>(i);
                dict[dictKey] = ConvertFromString<TValue>(GetRawValue(section, key + "." + ToString(dictKey)));
            }
            return dict;
        }

        template<typename TKey, typename TValue>
        void SetValueDictionary(const std::string& section, const std::string& key, const std::map<TKey, TValue>& value) {
            for (const auto& pair : value) {
                SetValue(section, key + "." + ToString(pair.first), pair.second);
            }
        }

        template<typename TKey, typename TValue>
        void SetValueDictionaryArray(const std::string& section, const std::string& key, const std::map<TKey, std::vector<TValue>>& value) {
            for (const auto& pair : value) {
                SetValueArray(section, key + "." + ToString(pair.first), pair.second);
            }
        }

        template<typename T>
        void SetValue(const std::string& section, const std::string& key, const T& value) {
            SetRawValue(section, key, ConvertToString(value));
        }

        template<typename TKey, typename TValue>
        std::map<TKey, std::vector<TValue>> GetValueDictionaryArray(const std::string& section, std::string key) {
            std::transform(key.begin(), key.end(), key.begin(), ::tolower);
            std::map<TKey, std::vector<TValue>> dict;
            for (int i = 0; i < static_cast<int>(TKey::Count); i++) {
                TKey dictKey = static_cast<TKey>(i);
                dict[dictKey] = GetValueArray<TValue>(section, key + "." + ToString(dictKey));
            }
            return dict;
        }

        template<typename T>
        void SetValueArray(const std::string& section, const std::string& key, const std::vector<T>& values) {
            std::string arrayString = "";
            char sep = GetArraySeparator<T>();
            for (size_t i = 0; i < values.size(); i++) {
                arrayString += ConvertToString(values[i]);
                if (i < values.size() - 1) arrayString += sep;
            }
            SetRawValue(section, key, arrayString);
        }

        void SaveToFile(const std::string& filePath);
    };

}
}