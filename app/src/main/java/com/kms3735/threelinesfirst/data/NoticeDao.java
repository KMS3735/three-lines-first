package com.kms3735.threelinesfirst.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

/** 모든 메서드는 메인 스레드가 아닌 곳에서 부른다 (allowMainThreadQueries를 쓰지 않음) */
@Dao
public interface NoticeDao {
    /** 최근에 찍은 안내문부터, 할 일 진행 개수와 함께 */
    @Query("SELECT n.*, "
            + "(SELECT COUNT(*) FROM todo t WHERE t.noticeId = n.id) AS todoTotal, "
            + "(SELECT COUNT(*) FROM todo t WHERE t.noticeId = n.id AND t.done = 1) AS todoDone "
            + "FROM notice n ORDER BY n.createdAt DESC")
    List<NoticeItem> getList();

    @Query("SELECT * FROM notice WHERE id = :id")
    Notice get(long id);

    /** 다시 켠 뒤 마감일 알림을 다시 예약할 때 */
    @Query("SELECT * FROM notice WHERE deadline IS NOT NULL AND deadline != ''")
    List<Notice> getWithDeadline();

    @Query("SELECT * FROM todo WHERE noticeId = :noticeId ORDER BY id")
    List<Todo> getTodos(long noticeId);

    @Insert
    long insert(Notice notice);

    @Update
    void update(Notice notice);

    @Insert
    void insertTodos(List<Todo> todos);

    @Query("DELETE FROM todo WHERE noticeId = :noticeId")
    void deleteTodos(long noticeId);

    /** 체크박스 상태 */
    @Update
    void updateTodo(Todo todo);

    @Delete
    void delete(Notice notice);

    /** 요약 결과와 할 일을 한 번에 저장한다. 다시 요약하면 할 일을 새로 바꾼다 */
    @Transaction
    default void saveSummary(Notice notice, List<Todo> todos) {
        update(notice);
        deleteTodos(notice.id);
        insertTodos(todos);
    }
}
