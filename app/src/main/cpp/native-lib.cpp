#include <jni.h>
#include <string>
#include <unistd.h>
#include "client/crashpad_client.h"
#include "client/crash_report_database.h"
#include "client/settings.h"


using namespace base;
using namespace crashpad;
using namespace std;

void crash() {
    *(volatile int *) 0 = 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_liulishuo_sprout_crashpad_SproutCrashManager_initializeCrashpad(JNIEnv *env, jobject thiz,
                                                                         jstring handler_path) {
    string dataDir = "/data/data/com.example.androidcrasher";

    const char *handlerPath = env->GetStringUTFChars(handler_path, 0);
    // Crashpad file paths
    FilePath handler(handlerPath);
    FilePath reportsDir(dataDir + "/crashpad");
    FilePath metricsDir(dataDir + "/crashpad");

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
    FilePath attachment(dataDir + "/files/attachment.txt");
    attachments.push_back(attachment);

    // Start Crashpad crash handler
    static CrashpadClient *client = new CrashpadClient();
    bool status = client->StartHandlerAtCrash(handler, reportsDir, metricsDir, url, annotations,
                                              arguments, attachments);

    env->ReleaseStringUTFChars(handler_path, handlerPath);
    return status;

}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_liulishuo_sprout_crashpad_SproutCrashManager_crash(JNIEnv *env, jobject thiz) {
    crash();
    return true;
}