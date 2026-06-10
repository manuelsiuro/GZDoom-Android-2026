#include "FileSAF.h"
#include "FileJNI.h"
#include "FileCache.h"
#include "Utils.h"

#include <stdio.h>
#include <dlfcn.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <libgen.h>

#include <string>
#include <map>
#include <vector>
#include <set>

#include <android/log.h>

#if 0
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO,"FileSAF NDK", __VA_ARGS__))
#else
#define LOGI(...)
#endif

// Use to disable interception
#if 1

// Stores paths which do not exist
static std::map<std::string, int> invalidPaths;
bool cacheInvalidPaths = true;

extern "C" void clearUserFilesFromCache(int lock);

extern "C"
{

// Get the real OS function
	void *loadRealFunc(const char *name)
	{
		static void *libc = NULL;

		if(libc == NULL)
		{
			libc = dlopen("libc.so", 0);

			if(!libc)
			{
				LOGI("ERROR LIBC NOT LOADED");
			}

			LOGI("fopen = %p", dlsym(libc, "fopen"));
			LOGI("__open_2 = %p", dlsym(libc, "__open_2"));
			LOGI("open = %p", dlsym(libc, "open"));
			LOGI("fclose = %p", dlsym(libc, "fclose"));
			LOGI("stat = %p", dlsym(libc, "stat"));
			LOGI("access = %p", dlsym(libc, "access"));
			LOGI("opendir = %p", dlsym(libc, "opendir"));
		}

		void *func;

		if(!libc)  // Could not open libc, bad
			func = dlsym(RTLD_NEXT, name);
		else
			func = dlsym(libc, name);

		if(func == NULL)
		{
			LOGI("ERROR, func %s not loaded, this is bad, really bad, seg fault ahead..", name);
		}

		return func;
	}

// Returns true is exists
	bool checkPathExistsCache(const char *path)
	{
		bool exists = false;
		const char *parent = dirname(path);
		std::string parentString = parent;

		if(parentString.length() > 2)  // Check valid path
		{
			// LOGI("parent = %s", parentString.c_str());
			// See if we have already checked the parent path
			if(invalidPaths.find(parentString) == invalidPaths.end())
			{
				// Get real 'stat' function
				static int (*stat_real)(const char *path, struct stat * statbuf) = NULL;

				if(stat_real == NULL)
					stat_real = (int (*)(const char *path, struct stat * statbuf)) loadRealFunc("stat");

				struct stat st;

				if(!stat_real(parentString.c_str(), &st))  // Return 0 for exists
				{
					// DOES exist
					invalidPaths[parentString] = 1;
					LOGI("DOES EXISTS");
					exists = true;
				}
				else
				{
					// Does NOT exist
					invalidPaths[parentString] = 0;
					LOGI("DOES NOT EXISTS");
				}
			}
			else
			{
				int parentExists = invalidPaths[parentString];

				if(parentExists)
					exists = true;
			}
		}
		else
		{
			exists = true;
		}

		return exists;
	}

//------------------------
// open INTERCEPT
// The only reason this is here is because one engine I use uses 'open' to test the presence of a file
// There could be weird bugs with this..
//------------------------
	int open(const char *path, int oflag, mode_t modes)
	{
		LOGI("open %s, %d %d", path, oflag, modes);

		// Remove relative paths (../ etc)
		std::string fullFilename = getCanonicalPath(path);

		bool inSAF = isInSAF(fullFilename);

		if(inSAF)
		{
			const char *mode = "r";

#define O_RDONLY        00
#define O_WRONLY        01
#define O_RDWR          02

			// Dark places uses raw open to write to files, check here
			// NOTE 'append' mode will not work, it will always truncate!
			if(oflag & (O_WRONLY | O_RDWR))
				mode = "w";

			// fd = -1, not in SAF area, fd = 0, failed to open. Otherwise valid file
			int fd = FileCache_getFd(fullFilename.c_str(), mode, FileJNI_fopen);

			if(fd > 0)
				return fd;
			else
				return -1;
		}
		else   // Not in SAF, normal access
		{
			// Load the real function

			static int (*open_real)(const char *path, int oflag, mode_t modes) = NULL;

			if(open_real == NULL)
				open_real = (int (*)(const char *path, int oflag, mode_t modes)) loadRealFunc("open");

			return open_real(path, oflag, modes);
		}
	}

// Android fcntl.h calls this function instead
	int __open_2(const char *path, int oflag, mode_t modes)
	{
		return open(path, oflag, modes);
	}

	/*
	    static int fopenGetFd(const char * filename, const char * mode)
	    {
	        static int(*open_real)(const char *path, int oflag, mode_t modes) = NULL;

	        if(open_real == NULL)
	            open_real = (int(*)(const char *path, int oflag, mode_t modes))loadRealFunc("open");

	        return open_real(filename, 0, 0);
	    }
	*/

//------------------------
// fopen INTERCEPT
//------------------------
	/*
	   Check if file is in SAF area and call Java to get FD if so
	*/
	FILE *fopen(const char *filename, const char *mode)
	{
		LOGI("fopen %s, %s", filename, mode);

		if(filename == NULL || mode == NULL)
		{
			LOGI("fopen: filename or mode is null");
			return NULL;
		}

		FILE *file = NULL;

		// Remove relative paths (../ etc)
		std::string fullFilename = getCanonicalPath(filename);

		//LOGI("fopen: file = %s, mode = %s", fullFilename.c_str(), mode);

		// Check if in SAF
		bool inSAF = isInSAF(fullFilename);

		if(inSAF)
		{
			int fd = FileCache_getFd(fullFilename.c_str(), mode, FileJNI_fopen);

			LOGI("fopen: file = %s, mode = %s, fd = %d", fullFilename.c_str(), mode, fd);

			if(fd > 0)    // File was in SAF area
			{
				file = fdopen(fd, mode);

				if(file == NULL)
					LOGI("ERROR fdopen returned NULL");
			}
		}
		else   // Not in SAF, normal access
		{
			// Load the real function
			static FILE *(*fopen_real)(const char *, const char *) = NULL;

			if(fopen_real == NULL)
				fopen_real = (FILE * (*)(const char *, const char *)) loadRealFunc("fopen");

			file = fopen_real(filename, mode);
		}

		return file;
	}


//------------------------
// fclose INTERCEPT
//------------------------
	int fclose(FILE *file)
	{
		LOGI("fclose %p", file);

		FileCache_closeFile(file);

		static int (*fclose_real)(FILE * file) = NULL;

		if(fclose_real == NULL)
			fclose_real = (int (*)(FILE * file)) loadRealFunc("fclose");

		return fclose_real(file);
	}

//------------------------
// close INTERCEPT
//------------------------
	int close(int fd)
	{
		LOGI("close %d", fd);

		FileCache_closeFd(fd);

		static int (*close_real)(int) = NULL;

		if(close_real == NULL)
			close_real = (int (*)(int)) loadRealFunc("close");

		return close_real(fd);
	}

//------------------------
// mkdir INTERCEPT
//------------------------
	int mkdir(const char *path, mode_t mode)
	{
		// Clear the invalid path cache so we can read files in newly created folder
		if(cacheInvalidPaths)
		{
			invalidPaths.clear();
		}

		// Remove relative paths (../ etc)
		std::string fullPath = getCanonicalPath(path);

		LOGI("mkdir %s, %d. fullPath %s", path, mode, fullPath.c_str());

		// status = -1, not in SAF area, status = 0 is OK, status = 1 failed
		int status = FileJNI_mkdir(fullPath.c_str());

		if(status == 0)
		{
			return 0; // OK
		}
		else if(status == 1)
		{
			return -1; // Failed
		}
		else   // Not in SAF, use real function
		{
			static int (*mkdir_real)(const char *path, mode_t mode) = NULL;

			if(mkdir_real == NULL)
				mkdir_real = (int (*)(const char *path, mode_t mode)) loadRealFunc("mkdir");

			return mkdir_real(path, mode);
		}
	}


//------------------------
// stat INTERCEPT
//------------------------
	int stat(const char *path, struct stat *statbuf)
	{
		LOGI("stat %s", path);

		std::string fullFilename = getCanonicalPath(path);

		bool inSAF = isInSAF(fullFilename);

		if(inSAF)
		{
			// Try to get an fd
			int fd = FileCache_getFd(fullFilename.c_str(), "r", FileJNI_fopen);

			if(fd > 0)    // File was in SAF area
			{
				fstat(fd, statbuf);
				LOGI("stat size = %ld, mode = %d", statbuf->st_size, statbuf->st_mode);
				close(fd);
				return 0;
			}
			else
			{
				return -1;
			}
		}
		else
		{
			static int (*stat_real)(const char *path, struct stat * statbuf) = NULL;

			if(stat_real == NULL)
				stat_real = (int (*)(const char *path, struct stat * statbuf)) loadRealFunc("stat");

			if(cacheInvalidPaths)
			{
				if(checkPathExistsCache(path) == false)
					return -1; // Parent does not exist, so file must not exist
			}

			return stat_real(path, statbuf);
		}

	}

//------------------------
// remove INTERCEPT
//------------------------
	int remove(const char *path)
	{
		LOGI("remove %s", path);

		std::string fullFilename = getCanonicalPath(path);

		bool inSAF = isInSAF(fullFilename);

		if(inSAF)
		{
            // Remove all user files from cache
            clearUserFilesFromCache(1);

			return FileJNI_delete(path);
		}
		else
		{
			static int (*remove_real)(const char *path) = NULL;

			if(remove_real == NULL)
				remove_real = (int (*)(const char *path)) loadRealFunc("remove");

			return remove_real(path);
		}
	}


//------------------------
// access INTERCEPT
//------------------------
	int access(const char *pathname, int mode)
	{
		LOGI("access %s", pathname);

		std::string fullFilename = getCanonicalPath(pathname);

		bool inSAF = isInSAF(fullFilename);

		if(inSAF)
		{
			// TODO! Check mode for access bits
			if(FileJNI_exists(fullFilename.c_str()))
				return 0;
			else
				return -1;
		}
		else
		{
			static int (*access_real)(const char *pathname, int mode) = NULL;

			if(access_real == NULL)
				access_real = (int (*)(const char *pathname, int mode)) loadRealFunc("access");

			return access_real(pathname, mode);
		}
	}

	class DIR_SAF
	{
	public:
		int position;
		std::vector<struct dirent> items;
	};

	std::set<DIR_SAF *> openDIRS;

//------------------------
// opendir INTERCEPT
//------------------------
	DIR *opendir(const char *name)
	{
		LOGI("opendir %s", name);

		std::string fullFilename = getCanonicalPath(name);

		bool inSAF = isInSAF(fullFilename);

		if(inSAF)
		{
			std::vector <std::string> items = FIleJNI_opendir(fullFilename.c_str());

			if(items.size() > 0)
			{
				// Create our own DIR object and fill with the data we need
				DIR_SAF *dirSaf = new DIR_SAF();
				dirSaf->position = 0;

				// Copy the items to out DIR object
				for(auto it = begin(items); it != end(items); ++it)
				{
					struct dirent d = {};

					// The first character of the name is the type (F = file, D = directory)
					std::string type = it->substr(0, 1);
					std::string name = it->substr(1);

					LOGI("Adding %s, type = %s", name.c_str(), type.c_str());

					if(type == "F")
						d.d_type = DT_REG;
					else if(type == "D")
						d.d_type = DT_DIR;
					else
						d.d_type = DT_UNKNOWN;

					// Copy name
					strcpy(d.d_name, name.c_str());

					// Add the item
					dirSaf->items.push_back(d);
				}

				// Add it to our list to remember this is ours
				openDIRS.insert(dirSaf);

				return (DIR *) dirSaf;
			}
			else
			{
				return NULL;
			}
		}
		else
		{
			static DIR *(*opendir_real)(const char *name) = NULL;

			if(opendir_real == NULL)
				opendir_real = (DIR * (*)(const char *name)) loadRealFunc("opendir");

			return opendir_real(name);
		}
	}


//------------------------
// readdir INTERCEPT
//------------------------
	struct dirent *readdir(DIR *dirp)
	{
		LOGI("readdir %p", dirp);

		DIR_SAF *dirSafe = (DIR_SAF *) dirp; // TODO sort out C style casts

		std::set<DIR_SAF *>::iterator it = openDIRS.find(dirSafe);

		if(it != openDIRS.end())     // It is in out list
		{
			if(dirSafe->position < dirSafe->items.size())
			{
				return &dirSafe->items.at(dirSafe->position++);
			}
			else
			{
				return NULL;
			}
		}
		else   // Not in our list,  use system the version
		{
			static struct dirent *(*readdir_real)(DIR * dirp) = NULL;

			if(readdir_real == NULL)
				readdir_real = (struct dirent * (*)(DIR * dirp)) loadRealFunc("readdir");

			return readdir_real(dirp);
		}
	}

//------------------------
// closedir INTERCEPT
//------------------------
	int closedir(DIR *dirp)
	{
		LOGI("closedir %p", dirp);

		DIR_SAF *dirSafe = (DIR_SAF *) dirp; // TODO sort out C style casts

		std::set<DIR_SAF *>::iterator it = openDIRS.find(dirSafe);

		if(it != openDIRS.end())     // It is in out list
		{
			openDIRS.erase(dirSafe);
			delete dirSafe;
			return 0;
		}
		else   // Not in our list, use system the version
		{
			static int (*closedir_real)(DIR * dirp) = NULL;

			if(closedir_real == NULL)
				closedir_real = (int (*)(DIR * dirp)) loadRealFunc("closedir");

			return closedir_real(dirp);
		}
	}

	int scandir(const char *dirp,
	            struct dirent ***namelist,
	            int (*filter)(const struct dirent *),
	            int (*compar)(const struct dirent **, const struct dirent **))
	{
		LOGI("scandir %s", dirp);

		std::string fullFilename = getCanonicalPath(dirp);

		bool inSAF = isInSAF(fullFilename);

		if(inSAF)
		{
			// Use opendir to find the files. opendir should actually return a DIR_SAF as we are in SAF
			DIR_SAF *dirSAF = (DIR_SAF *) opendir(fullFilename.c_str());

			if(dirSAF)
			{
				int numberFiles = 0;

				if(dirSAF->items.size() > 0)
				{
					// Create pointer to list of all possible files (before filtering)
					*namelist = (dirent **) malloc(sizeof(dirent *) * dirSAF->items.size());

					// Use this to make code easier to read
					dirent **namelistPrt = *namelist;

					// Scan all files found my opendir and filter as necessary
					for(int n = 0; n < dirSAF->items.size(); n++)
					{
						// Get the dirent from our list
						const struct dirent *ent = &dirSAF->items.at(n);

						// Check if filtering is needed
						if(filter == NULL || filter(ent) != 0)
						{
							// Create the dirent
							namelistPrt[numberFiles] = (dirent *) malloc(sizeof(dirent));

							//Copy info we have to the dirent, NOTE not everything is implemented here
							strcpy(namelistPrt[numberFiles]->d_name, ent->d_name);
							namelistPrt[numberFiles]->d_type = ent->d_type;

							numberFiles++;
						}
					}
				}

				closedir((DIR *) dirSAF);

				return numberFiles;
			}
			else
			{
				return 0;
			}
		}
		else
		{
			static int (*scandir_real)(const char *,
			                           struct dirent ***,
			                           int (*)(const struct dirent *),
			                           int (*)(const struct dirent **, const struct dirent **)) = NULL;

			if(scandir_real == NULL)
				scandir_real = (int (*)(const char *,
				                        struct dirent ***,
				                        int (*)(const struct dirent *),
				                        int (*)(const struct dirent **,
				                                const struct dirent **))) loadRealFunc("scandir");

			return scandir_real(dirp, namelist, filter, compar);
		}
	}


	int rename(const char *old_filename, const char *new_filename)
	{
		LOGI("rename %s -> %s", old_filename, new_filename);

		std::string fullFilenameOld = getCanonicalPath(old_filename);
		std::string fullFilenameNew = getCanonicalPath(new_filename);

		bool inSAFOld = isInSAF(fullFilenameOld);
		bool inSAFNew = isInSAF(fullFilenameNew);

		if(inSAFOld && inSAFNew)
		{
            // Remove all user files from cache
            clearUserFilesFromCache(1);

			return FileJNI_rename(old_filename, new_filename);
		}
		else
		{
			static int (*rename_real)(const char *old_filename, const char *new_filename) = NULL;

			if(rename_real == NULL)
				rename_real = (int (*)(const char *old_filename, const char *new_filename)) loadRealFunc("rename");

			return rename_real(old_filename, new_filename);
		}
	}
}
#endif