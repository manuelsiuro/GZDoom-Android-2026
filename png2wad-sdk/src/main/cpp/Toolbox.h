#pragma once

#include <string>
#include <vector>
#include <cmath>
#include <algorithm>
#include <optional>
#include <random>

struct Color {
    uint8_t a, r, g, b;
    Color(uint8_t a, uint8_t r, uint8_t g, uint8_t b) : a(a), r(r), g(g), b(b) {}
    Color(uint8_t r, uint8_t g, uint8_t b) : a(255), r(r), g(g), b(b) {}
    Color() : a(255), r(0), g(0), b(0) {}

    bool IsSameRGB(const Color& other) const {
        return r == other.r && g == other.g && b == other.b;
    }
    bool operator==(const Color& other) const {
        return r == other.r && g == other.g && b == other.b && a == other.a;
    }
};

struct Point {
    int X;
    int Y;

    Point(int x = 0, int y = 0) : X(x), Y(y) {}

    float Distance(const Point& other) const {
        return std::sqrt(std::pow(X - other.X, 2) + std::pow(Y - other.Y, 2));
    }

    Point Add(const Point& other) const {
        return Point(X + other.X, Y + other.Y);
    }

    Point Mult(const Point& multiplier) const {
        return Point(X * multiplier.X, Y * multiplier.Y);
    }

    bool operator==(const Point& other) const {
        return X == other.X && Y == other.Y;
    }

    static Point Empty() {
        return Point(0, 0);
    }
};

class Toolbox {
private:
    static std::mt19937 RNG;
public:
    static std::optional<Color> GetColorFromString(std::string colorString);
    static int RandomInt(int maxValue);
    static int RandomInt(int minValue, int maxValue);

    template <typename T>
    static T RandomFromArray(const std::vector<T>& array) {
        if (array.empty()) return T();
        return array[RandomInt(array.size())];
    }

    template <typename T>
    static T RandomFromList(const std::vector<T>& list) {
        if (list.empty()) return T();
        return list[RandomInt(list.size())];
    }

    static int Clamp(int value, int minValue, int maxValue);
    static std::string Trim(const std::string& str);
};
