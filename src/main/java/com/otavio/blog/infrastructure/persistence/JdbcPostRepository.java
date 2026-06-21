package com.otavio.blog.infrastructure.persistence;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.Category;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.post.PostRepository;
import com.otavio.blog.domain.post.PostStatus;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Slug;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPostRepository implements PostRepository {
    
    private final JdbcTemplate jdbcTemplate;

    public JdbcPostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Post save(Post post) {
        if (existsById(post.getId())) {
            return update(post);
        } else {
            return insert(post);
        }
    }

    private Post insert(Post post) {
        String sql = """
            INSERT INTO posts (id, title, slug, description, status, author_id, category_id, published_at, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
            post.getId().getValue(),
            post.getTitle(),
            post.getSlug().getValue(),
            post.getDescription(),
            post.getStatus().name(),
            post.getAuthorId().getValue(),
            post.getCategoryId().getValue(),
            toTimestamp(post.getPublishedAt()),
            toTimestamp(post.getCreatedAt()),
            toTimestamp(post.getUpdatedAt()),
            toTimestamp(post.getDeletedAt())
        );

        // Insert tags
        saveTags(post);

        return post;
    }

    private Post update(Post post) {
        String sql = """
            UPDATE posts 
            SET title = ?, slug = ?, description = ?, status = ?, 
                category_id = ?, published_at = ?, updated_at = ?, deleted_at = ?
            WHERE id = ?
            """;

        jdbcTemplate.update(sql,
            post.getTitle(),
            post.getSlug().getValue(),
            post.getDescription(),
            post.getStatus().name(),
            post.getCategoryId().getValue(),
            toTimestamp(post.getPublishedAt()),
            toTimestamp(post.getUpdatedAt()),
            toTimestamp(post.getDeletedAt()),
            post.getId().getValue()
        );

        // Delete and reinsert tags
        deleteTags(post.getId());
        saveTags(post);

        return post;
    }

    private void saveTags(Post post) {
        if (post.getTags().isEmpty()) {
            return;
        }

        String sql = "INSERT INTO post_tags (post_id, tag) VALUES (?, ?)";
        
        List<Object[]> batchArgs = post.getTags().stream()
            .map(tag -> new Object[]{post.getId().getValue(), tag})
            .toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void deleteTags(Id<Post> postId) {
        String sql = "DELETE FROM post_tags WHERE post_id = ?";
        jdbcTemplate.update(sql, postId.getValue());
    }

    @Override
    public Optional<Post> findById(Id<Post> id) {
        String sql = """
            SELECT p.* FROM posts p
            WHERE p.id = ? AND p.deleted_at IS NULL
            """;

        try {
            Post post = jdbcTemplate.queryForObject(sql, new PostRowMapper(), id.getValue());
            loadTags(post);
            return Optional.ofNullable(post);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Post> findBySlug(Slug slug) {
        String sql = """
            SELECT p.* FROM posts p
            WHERE p.slug = ? AND p.deleted_at IS NULL
            """;

        try {
            Post post = jdbcTemplate.queryForObject(sql, new PostRowMapper(), slug.getValue());
            loadTags(post);
            return Optional.ofNullable(post);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsBySlug(Slug slug) {
        String sql = "SELECT COUNT(*) FROM posts WHERE slug = ? AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, slug.getValue());
        return count != null && count > 0;
    }

    @Override
    public int findNextSlugSuffix(String baseSlug) {
        // Busca slugs que seguem o padrão base-slug-2, base-slug-3, etc
        String sql = """
            SELECT slug FROM posts 
            WHERE slug LIKE ? AND deleted_at IS NULL
            ORDER BY slug
            """;

        List<String> slugs = jdbcTemplate.queryForList(sql, String.class, baseSlug + "-%");
        
        if (slugs.isEmpty()) {
            return 2; // Primeiro sufixo
        }

        // Extrai o maior sufixo numérico
        int maxSuffix = 1;
        for (String slug : slugs) {
            String[] parts = slug.split("-");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                try {
                    int suffix = Integer.parseInt(lastPart);
                    if (suffix > maxSuffix) {
                        maxSuffix = suffix;
                    }
                } catch (NumberFormatException ignored) {
                    // Não é um número, ignora
                }
            }
        }

        return maxSuffix + 1;
    }

    @Override
    @Transactional
    public void delete(Id<Post> id) {
        String sql = "UPDATE posts SET deleted_at = NOW(), updated_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, id.getValue());
    }

    private boolean existsById(Id<Post> id) {
        String sql = "SELECT COUNT(*) FROM posts WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.getValue());
        return count != null && count > 0;
    }

    private void loadTags(Post post) {
        String sql = "SELECT tag FROM post_tags WHERE post_id = ? ORDER BY tag";
        List<String> tags = jdbcTemplate.queryForList(sql, String.class, post.getId().getValue());
        post.setTags(tags);
    }

    private Timestamp toTimestamp(java.time.Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static class PostRowMapper implements RowMapper<Post> {
        @Override
        public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
            Post post = new Post(Id.of(rs.getObject("id", UUID.class)));
            post.setTitle(rs.getString("title"));
            post.setSlug(Slug.of(rs.getString("slug")));
            post.setDescription(rs.getString("description"));
            post.setStatus(PostStatus.valueOf(rs.getString("status")));
            post.setAuthorId(Id.of(rs.getObject("author_id", UUID.class)));
            post.setCategoryId(Id.of(rs.getObject("category_id", UUID.class)));
            
            Timestamp publishedAt = rs.getTimestamp("published_at");
            post.setPublishedAt(publishedAt != null ? publishedAt.toInstant() : null);
            
            post.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            post.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            post.setDeletedAt(deletedAt != null ? deletedAt.toInstant() : null);
            
            return post;
        }
    }
}
