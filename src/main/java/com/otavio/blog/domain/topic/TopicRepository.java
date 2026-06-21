package com.otavio.blog.domain.topic;

import com.otavio.blog.domain.post.Post;
import com.otavio.blog.shared.domain.Id;

import java.util.List;
import java.util.Optional;

public interface TopicRepository {

    Topic save(Topic topic);

    Optional<Topic> findById(Id<Topic> id);

    List<Topic> findByPostIdOrderByOrderNumber(Id<Post> postId);

    int countByPostId(Id<Post> postId);

    /**
     * Incrementa order_number de todos os tópicos >= fromOrder para abrir espaço
     */
    void incrementOrdersFrom(Id<Post> postId, int fromOrder);

    void delete(Id<Topic> id);
}
