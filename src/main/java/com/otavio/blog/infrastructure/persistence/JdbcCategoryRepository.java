package com.otavio.blog.infrastructure.persistence;

import com.otavio.blog.domain.category.Category;
import com.otavio.blog.domain.category.CategoryRepository;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Slug;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCategoryRepository implements CategoryRepository {
    
    private final JdbcTemplate jdbcTemplate;

    public JdbcCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Category> findById(Id<Category> id) {
        String sql = "SELECT * FROM categories WHERE id = ? AND deleted_at IS NULL";
        
        try {
            Category category = jdbcTemplate.queryForObject(sql, new CategoryRowMapper(), id.getValue());
            return Optional.ofNullable(category);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Id<Category> id) {
        String sql = "SELECT COUNT(*) FROM categories WHERE id = ? AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.getValue());
        return count != null && count > 0;
    }

    private static class CategoryRowMapper implements RowMapper<Category> {
        @Override
        public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
            Category category = new Category(Id.of(rs.getObject("id", UUID.class)));
            category.setName(rs.getString("name"));
            category.setSlug(Slug.of(rs.getString("slug")));
            category.setDescription(rs.getString("description"));
            category.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            category.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return category;
        }
    }
}
