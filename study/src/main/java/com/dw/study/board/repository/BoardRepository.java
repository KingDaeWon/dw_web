package com.dw.study.board.repository;

import com.dw.study.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderByBoardIdDesc();

    /**
     * jpql
     * @return
     */
    @Query("select count(*) from Board")
    int getTotalCount();

    /**
     * custom 쿼리메소드 작성시 find이후 By를 꼭 작성한다.
     * @param pageable
     * @return
     */
    Page<Board> findAllByOrderByBoardIdDesc(Pageable pageable);

    List<Board> findByBoardContentLike(String boardContent);

    List<Board> findByBoardContentLikeAndMemberMemberNameLike(String content, String memberName);


}