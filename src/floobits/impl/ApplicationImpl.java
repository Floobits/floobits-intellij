package floobits.impl;

import com.intellij.openapi.application.ApplicationInfo;

public class ApplicationImpl {
    public static String getClientName() {
        return ApplicationInfo.getInstance().getVersionName();
    }
}
