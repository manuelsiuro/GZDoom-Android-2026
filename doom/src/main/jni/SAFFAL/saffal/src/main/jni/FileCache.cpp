
#include "FileCache.h"
#include "FileJNI.h"

#include <sys/types.h>
#include <unistd.h>

#include <map>
#include <string>
#include <vector>
#include <pthread.h>

#include <android/log.h>

static const int MAXIMUM_CACHED_FILES = 256;

#if 0
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO,"FileCache NDK", __VA_ARGS__))
#else
#define LOGI(...)
#endif

#define LOGIALWAYS(...) ((void)__android_log_print(ANDROID_LOG_INFO,"FileCache NDK", __VA_ARGS__))

static pthread_mutex_t lock;

#if 1
#define MUTEX_LOCK  pthread_mutex_lock(&lock);
#define MUTEX_UNLOCK  pthread_mutex_unlock(&lock);
#else
#define MUTEX_LOCK
#define MUTEX_UNLOCK
#endif

extern "C" void* loadRealFunc(const char * name); // In FileSAF.cpp

static std::map<int, std::string> cacheActive;
static std::map<std::string, int> cacheFree;

void FileCache_init()
{
	pthread_mutex_init(&lock, NULL);
}

extern "C"  void clearUserFilesFromCache(int mutextLock)
{
    if(mutextLock) {
        MUTEX_LOCK
    }
    for (auto it = cacheFree.begin(); it != cacheFree.end();)
    {
        if (it->first.find("user_files") != std::string::npos)
        {
            LOGIALWAYS("Removing user_file from cache: %s", it->first.c_str());

            static int (*close_real)(int) = NULL;

            if(close_real == NULL)
                close_real = (int(*)(int))loadRealFunc("close");

            close_real(it->second);

            it = cacheFree.erase(it);
        } else {
            ++it;
        }
    }
    if(mutextLock) {
        MUTEX_UNLOCK
    }
}

int FileCache_getFd(const char * filename, const char * mode, int (*openFunc)(const char * filename, const char * mode))
{
	MUTEX_LOCK

	int fd = 0;

	static char fileTag[256]; // Need to include the filename AND the mode, can not mix modes
	snprintf(fileTag, 256, "%s - %s", filename, mode);

	// Check if writing, if so DO NOT CACHE, also do not cache in user_files
	//if(strchr(mode, 'w') || strchr(mode, 'a') || strstr(filename, "user_files"))
    if(strchr(mode, 'w') || strchr(mode, 'a'))
	{
        clearUserFilesFromCache(0);
		fd = openFunc(filename, mode);
	}
	else
	{
		// Check if file is in our cache
		if(cacheFree.find(fileTag) == cacheFree.end())       // not found
		{
			//LOGI("FileCache_getFd NOT FOUND");
			fd = openFunc(filename, mode);

			if(fd > 0)
			{
				LOGI("FileCache_getFd %s(%s) NOT FOUND, new fd = %d", filename, mode, fd);
			}
		}
		else  // found
		{
			// Get cached fd
			fd = cacheFree[fileTag];

			//Remove from free cache
			cacheFree.erase(fileTag);

			LOGI("FileCache_getFd %s(%s) FOUND, fd = %d", filename, mode, fd);

			// Reset the fd position
			lseek(fd, 0, SEEK_SET);
		}

		// FD should always be unique, so map works
		if(fd > 0)
		{
			cacheActive[fd] = fileTag;
		}
	}

	MUTEX_UNLOCK

	return fd;
}


static void closeFd(int fd)
{
	MUTEX_LOCK

	if(cacheActive.find(fd) == cacheActive.end())      // not found
	{
		LOGI("FileCache_closeFile NOT FOUND");
	}
	else
	{
		LOGI("FileCache_closeFile FOUND  %s", cacheActive[fd].c_str());

#if 1

		// If we haven't already got a cached of this file, add it now
		if(cacheFree.find(cacheActive[fd]) == cacheFree.end())
		{
			// Free a file when over the cached limit
			if(cacheFree.size() > MAXIMUM_CACHED_FILES)
			{
				// NOTE: This essentially take a random file from the cache, whatever happens to be sorted to the front.
				// Better method would be to have them sorted in last used in another list
				std::pair<std::string, int> firstEntry = *cacheFree.begin();
				LOGI("FileCache_closeFile Removing from cache: %s, fd = %d", firstEntry.first.c_str(), firstEntry.second);

				static int (*close_real)(int) = NULL;

				if(close_real == NULL)
					close_real = (int(*)(int))loadRealFunc("close");

				// Close the fd
				close_real(firstEntry.second);

				// Remove
				cacheFree.erase(firstEntry.first);
			}

			// Make a copy of the FD so the one held in FILE can be close
			int fdCopy = dup(fd);

			// Save new fd to the free cache
			LOGI("FileCache_closeFile CACHEING %d, cache size = %d", fdCopy, cacheFree.size());

			cacheFree[cacheActive[fd]] = fdCopy;
		}

#endif
		// Remove the old fd from the current active cache
		cacheActive.erase(fd);
	}

	MUTEX_UNLOCK
}

int FileCache_closeFd(int fd)
{
	closeFd(fd);
	return 0;
}

int FileCache_closeFile(FILE * file)
{
	int fd = fileno(file);
	closeFd(fd);
	return 0;
}


