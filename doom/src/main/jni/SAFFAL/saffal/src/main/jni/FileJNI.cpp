
#include "FileJNI.h"
#include "FileCache.h"
#include "Utils.h"

#include <android/log.h>
#include <jni.h>
#include <pthread.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO,"FileJNI NDK", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN,"FileJNI NDK", __VA_ARGS__))

static pthread_mutex_t lock;

#if 0
#define MUTEX_LOCK  pthread_mutex_lock(&lock);
#define MUTEX_UNLOCK  if(attached) (m_jvm)->DetachCurrentThread(); pthread_mutex_unlock(&lock);
#else
#define MUTEX_LOCK
#define MUTEX_UNLOCK
#endif


static JNIEnv* firstEnv = 0;
static JavaVM* m_jvm;

extern bool cacheInvalidPaths;

static bool getEnv(JNIEnv **jniEnv)
{
	bool attached = false;
	int status = 0;

	if(!m_jvm)
	{
		LOGI("ERROR, jvm for getEnv is NULL, make sure you load the library first");
		return false;
	}

	status = (m_jvm)->GetEnv((void **) jniEnv, JNI_VERSION_1_6);

	if(status == JNI_EDETACHED)
	{
		LOGI("getEnv: Thread not attached, attaching...");
		(m_jvm)->DetachCurrentThread();
		status = (m_jvm)->AttachCurrentThread(jniEnv, NULL);

		if(status < 0)
		{
			LOGI("getEnv: ERROR failed to attach current thread");
		}

		attached = true;
	}

	if(!*jniEnv)
		LOGI("ERROR, getEnv env is NULL");

	if(firstEnv == 0)
		firstEnv = *jniEnv;

	return attached;
}

static jclass FileJNI_cls;
static jmethodID fopen_method;
static jmethodID fclose_method;
static jmethodID mkdir_method;
static jmethodID exists_method;
static jmethodID opendir_method;
static jmethodID delete_method;
static jmethodID rename_method;

extern "C" __attribute__((visibility("default"))) jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	LOGI("JNI_OnLoad");
	m_jvm = vm;
	pthread_mutex_init(&lock, NULL);

	JNIEnv *env;
	vm->GetEnv((void **) &env, JNI_VERSION_1_4);

	jclass cls = (env)->FindClass("com/opentouchgaming/saffal/FileJNI");
	FileJNI_cls = (jclass)(env->NewGlobalRef(cls));

	fopen_method = (env)->GetStaticMethodID(FileJNI_cls, "fopen", "(Ljava/lang/String;Ljava/lang/String;)I");
	//fclose_method = (env)->GetStaticMethodID(FileJNI_cls, "fclose", "(I)I");
	mkdir_method = (env)->GetStaticMethodID(FileJNI_cls, "mkdir", "(Ljava/lang/String;)I");
	exists_method = (env)->GetStaticMethodID(FileJNI_cls, "exists", "(Ljava/lang/String;)I");
	delete_method = (env)->GetStaticMethodID(FileJNI_cls, "delete", "(Ljava/lang/String;)I");
	opendir_method = (env)->GetStaticMethodID(FileJNI_cls, "opendir", "(Ljava/lang/String;)[Ljava/lang/String;");
	rename_method = (env)->GetStaticMethodID(FileJNI_cls, "rename", "(Ljava/lang/String;Ljava/lang/String;)I");

	FileCache_init();

	return JNI_VERSION_1_6;
}


extern "C" void JNICALL Java_com_opentouchgaming_saffal_FileJNI_initSAFPaths(JNIEnv* env, jclass cls, jobjectArray SAFPaths, jint cacheNativeFs)
{
	cacheInvalidPaths = (cacheNativeFs != 0);
	LOGI("SAF: Cache invalid paths = %d", cacheInvalidPaths);

	clearSAFPaths();

	int count = env->GetArrayLength(SAFPaths);
	for(int i = 0; i < count; i++)
	{
		jstring pathStr = (jstring)env->GetObjectArrayElement(SAFPaths, i);
		const char* pathC = env->GetStringUTFChars(pathStr, 0);
		LOGI("SAF: Adding path[%d]: %s", i, pathC);
		addSAFPath(std::string(pathC));
		env->ReleaseStringUTFChars(pathStr, pathC);
		env->DeleteLocalRef(pathStr);
	}
}


int FileJNI_fopen(const char * filename, const char * mode)
{
	MUTEX_LOCK

	JNIEnv *env = NULL;
	bool attached = getEnv(&env);


	jstring filenameStr = (env)->NewStringUTF(filename);
	jstring modeStr = (env)->NewStringUTF(mode);

	// Call Java function
	// Returns -1 for invalid file in SAF. Else a valid FD
	int ret = (env)->CallStaticIntMethod(FileJNI_cls, fopen_method, filenameStr, modeStr);

    if(ret > 0)
        LOGW("Opened file from SAF: %s", filename);

	(env)->DeleteLocalRef(filenameStr);
	(env)->DeleteLocalRef(modeStr);

	MUTEX_UNLOCK
	return ret;
}

int FileJNI_fclose(int fd)
{
	MUTEX_LOCK

	JNIEnv *env = NULL;
	bool attached = getEnv(&env);

	int ret = (env)->CallStaticIntMethod(FileJNI_cls, fclose_method, fd);

	MUTEX_UNLOCK

	return ret;
}

int FileJNI_mkdir(const char * path)
{
	MUTEX_LOCK

	JNIEnv *env = NULL;
	bool attached = getEnv(&env);

	jstring pathStr = (env)->NewStringUTF(path);

	// Call Java function
	int ret = (env)->CallStaticIntMethod(FileJNI_cls, mkdir_method, pathStr);

	(env)->DeleteLocalRef(pathStr);

	MUTEX_UNLOCK
	return ret;
}

int FileJNI_exists(const char * path)
{
	MUTEX_LOCK

	JNIEnv *env = NULL;
	bool attached = getEnv(&env);

	jstring pathStr = (env)->NewStringUTF(path);

	// Call Java function
	int ret = (env)->CallStaticIntMethod(FileJNI_cls, exists_method, pathStr);

	(env)->DeleteLocalRef(pathStr);

	MUTEX_UNLOCK

	return ret;
}

int FileJNI_delete(const char * path)
{
	MUTEX_LOCK

	JNIEnv *env = NULL;
	bool attached = getEnv(&env);

	jstring pathStr = (env)->NewStringUTF(path);

	// Call Java function
	int ret = (env)->CallStaticIntMethod(FileJNI_cls, delete_method, pathStr);

	(env)->DeleteLocalRef(pathStr);

	MUTEX_UNLOCK

	return ret;
}

int FileJNI_rename(const char * oldFilename, const char * newFilename)
{
    MUTEX_LOCK

    JNIEnv *env = NULL;
    bool attached = getEnv(&env);

    jstring oldStr = (env)->NewStringUTF(oldFilename);
    jstring newStr = (env)->NewStringUTF(newFilename);

    // Call Java function
    int ret = (env)->CallStaticIntMethod(FileJNI_cls, rename_method, oldStr, newStr);

    (env)->DeleteLocalRef(oldStr);
    (env)->DeleteLocalRef(newStr);

    MUTEX_UNLOCK

    return ret;
}

std::vector<std::string> FIleJNI_opendir(const char * path)
{
	MUTEX_LOCK

	JNIEnv *env = NULL;
	bool attached = getEnv(&env);

	jstring pathStr = (env)->NewStringUTF(path);

	// Call Java function
	// Returns and array of items in the directory. Will return an array of zero items if not found (or empty)
	jobjectArray jniItems = (jobjectArray)(env)->CallStaticObjectMethod(FileJNI_cls, opendir_method, pathStr);

	(env)->DeleteLocalRef(pathStr);

	MUTEX_UNLOCK

	int size = env->GetArrayLength(jniItems);

	std::vector<std::string> items;

	for(int i = 0; i < size; i++)
	{
		jstring string = (jstring)env->GetObjectArrayElement(jniItems, i);
		const char* item = env->GetStringUTFChars(string, 0);
		items.push_back(item);
		env->ReleaseStringUTFChars(string, item);
		env->DeleteLocalRef(string);
	}

	return items;
}
