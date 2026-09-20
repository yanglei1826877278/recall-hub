package com.recallhub.common;

public final class Types {
    private Types() {}
    public enum EntryType { TODO, DIARY, NOTE, IDEA }
    public enum EntryStatus { ACTIVE, DONE, ARCHIVED }
    public enum SourceType { WEB, OPENCLAW, IMPORT, SYSTEM }
    public enum Channel { WEB, WECHAT, TELEGRAM, QQ, VOICE, UNKNOWN }
    public enum ReminderStatus { SCHEDULED, PROCESSING, SENT, FAILED, CANCELLED }
    public enum Appearance { LIGHT, DARK, SYSTEM }
}

