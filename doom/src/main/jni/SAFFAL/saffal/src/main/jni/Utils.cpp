#include "Utils.h"

#include <stdio.h>
#include <string>
#include <vector>

#include <android/log.h>
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO,"Utils NDK", __VA_ARGS__))


static std::vector<std::string> m_SAFPaths;

// This is from https://stackoverflow.com/questions/28659344/alternative-to-realpath-to-resolve-and-in-a-path
#define IS_SLASH(s) (s == '/')
static void ap_getparents(char *name)
{
	char *next;
	int l, w, first_dot;

	/* Four paseses, as per RFC 1808 */
	/* a) remove ./ path segments */
	for(next = name; *next && (*next != '.'); next++)
	{
	}

	l = w = first_dot = next - name;

	while(name[l] != '\0')
	{
		if(name[l] == '.' && IS_SLASH(name[l + 1])
		        && (l == 0 || IS_SLASH(name[l - 1])))
			l += 2;
		else
			name[w++] = name[l++];
	}

	/* b) remove trailing . path, segment */
	if(w == 1 && name[0] == '.')
		w--;
	else if(w > 1 && name[w - 1] == '.' && IS_SLASH(name[w - 2]))
		w--;

	name[w] = '\0';

	/* c) remove all xx/../ segments. (including leading ../ and /../) */
	l = first_dot;

	while(name[l] != '\0')
	{
		if(name[l] == '.' && name[l + 1] == '.' && IS_SLASH(name[l + 2])
		        && (l == 0 || IS_SLASH(name[l - 1])))
		{
			int m = l + 3, n;

			l = l - 2;

			if(l >= 0)
			{
				while(l >= 0 && !IS_SLASH(name[l]))
					l--;

				l++;
			}
			else
				l = 0;

			n = l;

			while((name[n] = name[m]))
				(++n, ++m);
		}
		else
			++l;
	}

	/* d) remove trailing xx/.. segment. */
	if(l == 2 && name[0] == '.' && name[1] == '.')
		name[0] = '\0';
	else if(l > 2 && name[l - 1] == '.' && name[l - 2] == '.'
	        && IS_SLASH(name[l - 3]))
	{
		l = l - 4;

		if(l >= 0)
		{
			while(l >= 0 && !IS_SLASH(name[l]))
				l--;

			l++;
		}
		else
			l = 0;

		name[l] = '\0';
	}
}

void clearSAFPaths()
{
	m_SAFPaths.clear();
}

void addSAFPath(std::string SAFPath)
{
	m_SAFPaths.push_back(SAFPath);
}

std::string getCanonicalPath(std::string path)
{
	// The above function works on c array, so copy
	char pathC[PATH_MAX];
	strncpy(pathC, path.c_str(), PATH_MAX);

	ap_getparents(pathC);

	return std::string(pathC);
}

bool isInSAF(std::string path)
{
	for(const auto& safPath : m_SAFPaths)
	{
		if(safPath.length() > 0 && path.rfind(safPath, 0) == 0)
		{
			return true;
		}
	}
	return false;
}