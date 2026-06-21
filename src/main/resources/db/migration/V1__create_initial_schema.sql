-- Create initial schema for H2 Database

-- Authors table
CREATE TABLE authors (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_authors_email ON authors(email);
CREATE INDEX idx_authors_deleted_at ON authors(deleted_at);

-- Categories table
CREATE TABLE categories (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_categories_slug ON categories(slug);

-- Posts table
CREATE TABLE posts (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    author_id UUID NOT NULL,
    category_id UUID NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES authors(id),
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX idx_posts_slug ON posts(slug);
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_category_id ON posts(category_id);
CREATE INDEX idx_posts_status ON posts(status);
CREATE INDEX idx_posts_published_at ON posts(published_at);

-- Post tags table (many-to-many)
CREATE TABLE post_tags (
    post_id UUID NOT NULL,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (post_id, tag),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_post_tags_tag ON post_tags(tag);

-- Topics table
CREATE TABLE topics (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    post_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    order_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_topics_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT chk_topics_order CHECK (order_number >= 1),
    CONSTRAINT uk_topic_order UNIQUE (post_id, order_number)
);

CREATE INDEX idx_topics_post_id ON topics(post_id);
CREATE INDEX idx_topics_order ON topics(post_id, order_number);

-- Images table
CREATE TABLE images (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    author_id UUID NOT NULL,
    s3_key VARCHAR(500) NOT NULL UNIQUE,
    url TEXT NOT NULL,
    alt_text VARCHAR(200),
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_images_author FOREIGN KEY (author_id) REFERENCES authors(id)
);

CREATE INDEX idx_images_author_id ON images(author_id);
CREATE INDEX idx_images_s3_key ON images(s3_key);

-- Topic images (association table)
CREATE TABLE topic_images (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    topic_id UUID NOT NULL,
    image_id UUID NOT NULL,
    position INT NOT NULL,
    caption VARCHAR(300),
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_topic_images_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
    CONSTRAINT fk_topic_images_image FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE,
    CONSTRAINT chk_topic_images_position CHECK (position >= 1),
    CONSTRAINT uk_topic_image UNIQUE (topic_id, image_id),
    CONSTRAINT uk_topic_image_position UNIQUE (topic_id, position)
);

CREATE INDEX idx_topic_images_topic_id ON topic_images(topic_id);
CREATE INDEX idx_topic_images_image_id ON topic_images(image_id);
CREATE INDEX idx_topic_images_position ON topic_images(topic_id, position);

-- Insert default category
INSERT INTO categories (id, name, slug, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Uncategorized', 'uncategorized', 'Default category for posts');

-- Insert default author (password: "password123")
INSERT INTO authors (id, name, email, password_hash, bio) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Otávio', 'otavio@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye7I0cDdP.eVJ2Y5.hZQdO9k9QWmwmyVW', 'Blog author');
