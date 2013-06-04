/*
Copyright Biel/Switzerland, 22. March 1999 by Christoph Dworzak
*/

#ifndef MUUDZ_H
#define MUUDZ_H

#include <android/log.h>

#define dappend(buf, size, format, args...) snprintf(buf+strlen(buf), size-strlen(buf), format, ##args)
#define dprintf(level, format, args...) if (level<2) { __android_log_print(ANDROID_LOG_DEBUG  , "libmuudz", format, ##args); }

#define INTTYPE int
#define VLONG long long
#define MAXSAMP (((long)1<<30)-1)
#define VFAT_IOCTL_GET_VOLUME_ID _IOR('r', 0x12, __u32)
#define MAXSTR 1024
#define RAWFILE

#endif
