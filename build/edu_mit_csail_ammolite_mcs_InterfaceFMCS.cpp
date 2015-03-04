#include <jni.h>
#include "edu_mit_csail_ammolite_mcs_InterfaceFMCS.h"
#include "../lib/fmcsR/config.h"

#include <stdexcept>
#include <sstream>
#include <iostream>
#include <fstream>
#include <string>
#include <list>
#include <vector>

#include "../lib/fmcsR/src/MCS.h"

using namespace std;
using namespace FMCS;

JNIEXPORT jint JNICALL Java_edu_mit_csail_ammolite_mcs_InterfaceFMCS_mcsSize
  (JNIEnv *env, jobject thisObj, jstring structureJStringOne, jstring structureJStringTwo)
{
	int atomMismatchLowerBound;
	int atomMismatchUpperBound;
	int bondMismatchLowerBound;
	int bondMismatchUpperBound;


	MCS::RunningMode runningMode = MCS::DETAIL;
	MCS::MatchType matchType = MCS::RING_SENSETIVE;

	MCSCompound compoundOne, compoundTwo;

	string structureStringOne;
	const char *s = env->GetStringUTFChars(structureJStringOne,NULL);
	structureStringOne = s;
	env->ReleaseStringUTFChars(structureJStringOne,s);
//	compoundOne.read(structureStringOne);

	return -8;
}

