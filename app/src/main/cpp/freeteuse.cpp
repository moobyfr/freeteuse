// Copyright (C) Yannick Le Roux.
// This file is part of Freeteuse.
//
//   Freeteuse is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   Freeteuse is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with Freeteuse.  If not, see <http://www.gnu.org/licenses/>.

#include <alloca.h>
#include <jni.h>
#include "Rcu.hpp"

// ---------------------------------------------------
extern "C"
JNIEXPORT jlong
  JNICALL
Java_bzh_leroux_yannick_freeteuse_Freebox_jniCreateRcu (JNIEnv  __unused *env,
                                                        jobject __unused  j_freebox)
{
  return (jlong) new Rcu ();
}

// ---------------------------------------------------
extern "C"
JNIEXPORT void
JNICALL
Java_bzh_leroux_yannick_freeteuse_Freebox_jniConnectRcu (JNIEnv           *env,
                                                         jobject __unused  jfreebox,
                                                         jlong             jrcu,
                                                         jstring           jaddress,
                                                         jint              port)
{
  Rcu *rcu = (Rcu *) jrcu;

  if (rcu)
  {
    const char *caddress = env->GetStringUTFChars (jaddress, 0);

    rcu->Connect (caddress,
                  (uint16_t) port);

    env->ReleaseStringUTFChars(jaddress, caddress);
  }
}

// ---------------------------------------------------
extern "C"
JNIEXPORT void
JNICALL
Java_bzh_leroux_yannick_freeteuse_Freebox_jniDisconnectRcu (JNIEnv  __unused *env,
                                                            jobject __unused  jfreebox,
                                                            jlong             jrcu)
{
  Rcu *rcu = (Rcu *) jrcu;

  if (rcu)
  {
    delete rcu;
  }
}

// ---------------------------------------------------
extern "C"
JNIEXPORT void
JNICALL
Java_bzh_leroux_yannick_freeteuse_Freebox_jniPressRcuKey (JNIEnv  __unused *env,
                                                          jobject __unused  j_freebox,
                                                          jlong             jrcu,
                                                          jint              report_id,
                                                          jint              key_code,
                                                          jboolean          with_release)
{
  Rcu *rcu = (Rcu *) jrcu;

  if (rcu)
  {
    rcu->SendKeyPress ((uint8_t)  report_id,
                       (uint32_t) key_code,
                       (bool)     with_release);
  }
}

// ---------------------------------------------------
extern "C"
JNIEXPORT void
JNICALL
Java_bzh_leroux_yannick_freeteuse_Freebox_jniReleaseRcuKey (JNIEnv  __unused *env,
                                                            jobject __unused  j_freebox,
                                                            jlong             jrcu,
                                                            jint              report_id,
                                                            jint              key_code)
{
  Rcu *rcu = (Rcu *) jrcu;

  if (rcu)
  {
    rcu->SendKeyRelease ((uint8_t)  report_id,
                         (uint32_t) key_code);
  }
}

// ---------------------------------------------------
extern "C"
JNIEXPORT jstring JNICALL
Java_bzh_leroux_yannick_freeteuse_Freebox_jniReadRcuStatus (JNIEnv  *env,
                                                            jobject  j_freebox,
                                                            jlong    jrcu)
{
  Rcu *rcu = (Rcu *) jrcu;

  if (rcu)
  {
    const char *status  = rcu->ReadStatus ();
    jstring     jstatus = env->NewStringUTF (status);

    return jstatus;
  }

  return NULL;
}