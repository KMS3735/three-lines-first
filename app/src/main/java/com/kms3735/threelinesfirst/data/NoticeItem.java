package com.kms3735.threelinesfirst.data;

import androidx.room.Embedded;

/** 목록 한 줄: 안내문과 할 일 진행 개수 */
public class NoticeItem {
    @Embedded
    public Notice notice;
    public int todoTotal;
    public int todoDone;
}
