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


extern "C" {

JNIEXPORT jint JNICALL Java_edu_mit_csail_ammolite_mcs_InterfaceFMCS_mcsSize
  (JNIEnv *env, jobject thisObj, jstring structureJStringOne, jstring structureJStringTwo)
{
	int atomMismatchLowerBound = 0;
	int atomMismatchUpperBound = 0;
	int bondMismatchLowerBound = 0;
	int bondMismatchUpperBound = 0;

	int substructureNumLimit = 1;
    int userDefinedLowerBound = 0;
    int timeout = 30*1000; // Probably in milliseconds?


	MCS::RunningMode runningMode = MCS::FAST;
	MCS::MatchType matchType = MCS::DEFAULT;

	MCSCompound compoundOne, compoundTwo;

	string structureStringOne;

	const char *s1 = env->GetStringUTFChars(structureJStringOne,NULL);
	structureStringOne = s1;
	env->ReleaseStringUTFChars(structureJStringOne,s1);
	compoundOne.read(structureStringOne);

	string structureStringTwo;
	const char *s2 = env->GetStringUTFChars(structureJStringTwo,NULL);
	structureStringTwo = s2;
	env->ReleaseStringUTFChars(structureJStringTwo,s2);
	compoundTwo.read(structureStringTwo);


	MCS mcs(compoundOne, compoundTwo,
            userDefinedLowerBound, substructureNumLimit,
            atomMismatchLowerBound, atomMismatchUpperBound,
            bondMismatchLowerBound, bondMismatchUpperBound,
            matchType, runningMode, timeout);

	mcs.calculate();

	return mcs.size();
}

}
