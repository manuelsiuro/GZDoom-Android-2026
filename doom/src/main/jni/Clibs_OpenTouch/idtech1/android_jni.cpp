static const char *key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0Ty9fat4Mag+a/UAncpVM8lNDrAQxk754HupOlYbJt3ALv6Fqagjj2vzPK8570aALqw2XEk5JxPAazdTQJ+W5aEVM8N2Ij1SbqN/yF+HfqDG+hHfszddwAZzKzWUlAkkeqW6qiIEy4L/TTOgj2vQv24ix4YcpO3eea2Ltz2UDyq+o0+K1cOCMqtuGL/GQbFS92zp3dnH9CpgtWFsbvVarjntJWiI6RrZpqpTTsuZWckK1ztMBjzNNnD1w6QbgTRqoGU7xmsHImWjk5MtwxiDqKL1EFOBvQDqOXxVc/jmT8StqAjk1ItCWStvJLZTzejNoGdTisxBQT/P3Xyppo8/MwIDAQAB";
static const char *pkg = "com.opentouchgaming.deltatouch";
static unsigned char sha_data[20] = {0x23, 0x53, 0xff, 0x41, 0x16, 0xd4, 0x43, 0x7f, 0x43, 0xaf, 0x12, 0x19, 0x75, 0xaa, 0xd7, 0xb0, 0x5e, 0xee, 0xf5, 0xde};

#include "../android_jni_inc.cpp"

#include "../touch_interface_base.cpp" // Just include for now so I dont have to change all the make files