#include "Toolbox.h"
#include <sstream>
#include <iomanip>
#include <cctype>

std::mt19937 Toolbox::RNG{std::random_device{}()};

std::string Toolbox::Trim(const std::string& str) {
    size_t first = str.find_first_not_of(' ');
    if (std::string::npos == first) {
        return "";
    }
    size_t last = str.find_last_not_of(' ');
    return str.substr(first, (last - first + 1));
}

std::optional<Color> Toolbox::GetColorFromString(std::string colorString) {
    colorString = Trim(colorString);
    if (colorString.empty()) return std::nullopt;

    if (colorString[0] == '#') {
        colorString = colorString.substr(1);
        if (colorString.length() == 6) {
            int r = std::stoi(colorString.substr(0, 2), nullptr, 16);
            int g = std::stoi(colorString.substr(2, 2), nullptr, 16);
            int b = std::stoi(colorString.substr(4, 2), nullptr, 16);
            return Color(255, r, g, b);
        } else if (colorString.length() == 3) {
            std::string xR = colorString.substr(0, 1); xR += xR;
            std::string xG = colorString.substr(1, 1); xG += xG;
            std::string xB = colorString.substr(2, 1); xB += xB;
            int r = std::stoi(xR, nullptr, 16);
            int g = std::stoi(xG, nullptr, 16);
            int b = std::stoi(xB, nullptr, 16);
            return Color(255, r, g, b);
        }
    } else if (colorString.find(',') != std::string::npos) {
        std::stringstream ss(colorString);
        std::string item;
        std::vector<std::string> rgbStrings;
        while (std::getline(ss, item, ',')) {
            rgbStrings.push_back(Trim(item));
        }
        if (rgbStrings.size() >= 3) {
            try {
                int r = std::stoi(rgbStrings[0]);
                int g = std::stoi(rgbStrings[1]);
                int b = std::stoi(rgbStrings[2]);
                return Color(255, r, g, b);
            } catch (...) {
                return std::nullopt;
            }
        }
    } else {
        std::string lowerColor = colorString;
        std::transform(lowerColor.begin(), lowerColor.end(), lowerColor.begin(), ::tolower);
        if (lowerColor == "red") return Color(255, 255, 0, 0);
        if (lowerColor == "blue") return Color(255, 0, 0, 255);
        if (lowerColor == "green") return Color(255, 0, 128, 0);
        if (lowerColor == "white") return Color(255, 255, 255, 255);
        if (lowerColor == "black") return Color(255, 0, 0, 0);
        if (lowerColor == "yellow") return Color(255, 255, 255, 0);
        if (lowerColor == "lime") return Color(255, 0, 255, 0);
        if (lowerColor == "magenta") return Color(255, 255, 0, 255);
        if (lowerColor == "olive") return Color(255, 128, 128, 0);
    }
    return std::nullopt;
}

int Toolbox::RandomInt(int maxValue) {
    if (maxValue <= 0) return 0;
    std::uniform_int_distribution<int> dist(0, maxValue - 1);
    return dist(RNG);
}

int Toolbox::RandomInt(int minValue, int maxValue) {
    if (minValue >= maxValue) return minValue;
    std::uniform_int_distribution<int> dist(minValue, maxValue - 1);
    return dist(RNG);
}

int Toolbox::Clamp(int value, int minValue, int maxValue) {
    return std::max(minValue, std::min(maxValue, value));
}
