package com.kms3735.threelinesfirst.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 할 일 체크박스 한 줄. 안내문을 지우면 함께 지워진다 */
@Entity(tableName = "todo",
        foreignKeys = @ForeignKey(entity = Notice.class, parentColumns = "id", childColumns = "noticeId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("noticeId"))
public class Todo {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long noticeId;
    public String content;
    public boolean done;
}
