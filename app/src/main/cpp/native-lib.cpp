#include <jni.h>
#include <string>
#include <unistd.h>
#include "client/crashpad_client.h"
#include "client/crash_report_database.h"
#include "client/settings.h"


using namespace base;
using namespace crashpad;
using namespace std;

std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}


void crash() {
    *(volatile int *) 0 = 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_liulishuo_sprout_crashpad_SproutCrashManager_initializeCrashpad(JNIEnv *env, jobject thiz,
                                                                         jstring handler_path,
                                                                         jstring dir) {
    string dataDir = jstring2string(env,dir);

    const char *handlerPath = env->GetStringUTFChars(handler_path, 0);
//    const char *dataDir = env->GetStringUTFChars(dir, 0);
    // Crashpad file paths
    FilePath handler(handlerPath);
    FilePath reportsDir(dataDir);
    FilePath metricsDir(dataDir);

    string url = "http://fred.bugsplat.com/post/bp/crash/crashpad.php";//

    map<string, string> annotations;
    annotations["format"] = "minidump";           // Required: Crashpad setting to save crash as a minidump
    annotations["database"] = "252019161_qq_com";             // Required: BugSplat appName
    annotations["product"] = "AndroidCrasher"; // Required: BugSplat appName
    annotations["version"] = "1.0.1";             // Required: BugSplat appVersion
    annotations["key"] = "Samplekey";            // Optional: BugSplat key field
    annotations["user"] = "fred@bugsplat.com";    // Optional: BugSplat user email
    annotations["list_annotations"] = "Sample comment"; // Optional: BugSplat crash description

    // Crashpad arguments
    vector<string> arguments;
    arguments.push_back("--no-rate-limit");

    // Crashpad local database
    unique_ptr<CrashReportDatabase> crashReportDatabase = CrashReportDatabase::Initialize(
            reportsDir);
    if (crashReportDatabase == NULL) return false;

    // Enable automated crash uploads
    Settings *settings = crashReportDatabase->GetSettings();
    if (settings == NULL) return false;
    settings->SetUploadsEnabled(false);

    // File paths of attachments to be uploaded with the minidump file at crash time - default bundle limit is 2MB
    vector<FilePath> attachments;
    FilePath attachment(dataDir + "/attachment.txt");
    attachments.push_back(attachment);

    // Start Crashpad crash handler
    static CrashpadClient *client = new CrashpadClient();
    bool status = client->StartHandlerAtCrash(handler, reportsDir, metricsDir, url, annotations,
                                              arguments, attachments);

    env->ReleaseStringUTFChars(handler_path, handlerPath);
    return status;

}



extern "C"
JNIEXPORT jboolean JNICALL
Java_com_liulishuo_sprout_crashpad_SproutCrashManager_crash(JNIEnv *env, jobject thiz) {
    crash();
    return true;
}