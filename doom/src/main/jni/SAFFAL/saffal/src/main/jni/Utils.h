#include <string>
#include <utility>

/*
     Add a SAF root path to the list of monitored paths
*/
void addSAFPath(std::string safPath);

/*
     Clear all SAF root paths
*/
void clearSAFPaths();

/*
     Remove . ./ ../ etc from a path
*/
std::string getCanonicalPath(std::string path);

/*
    Return true is in SAF area
*/
bool isInSAF(std::string path);
