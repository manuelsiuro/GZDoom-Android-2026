
#include <vector>


int FileJNI_fopen(const char * filename, const char * mode);
int FileJNI_fclose(int fd);
int FileJNI_mkdir(const char * path);
int FileJNI_exists(const char * path);
int FileJNI_delete(const char * path);
int FileJNI_rename(const char * oldFilename, const char * newFilename);

std::vector<std::string> FIleJNI_opendir(const char * path);
