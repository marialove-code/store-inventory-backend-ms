-- 智搜 V2：商品语义检索向量表（需已启用 CREATE EXTENSION vector）
-- 在 inventory_store 库、建议用 postgres 超管执行，再授权给业务账号

CREATE TABLE IF NOT EXISTS goods_search_embedding (
    id              BIGSERIAL PRIMARY KEY,
    goods_id        BIGINT       NOT NULL,
    chunk_text      TEXT         NOT NULL,
    embedding       vector(1024) NOT NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_goods_search_embedding_goods_id UNIQUE (goods_id)
);

COMMENT ON TABLE goods_search_embedding IS '智搜V2：商品文本块与向量（一商品一块）';
COMMENT ON COLUMN goods_search_embedding.chunk_text IS '名+规格+品牌+分类拼接文本';
COMMENT ON COLUMN goods_search_embedding.embedding IS 'DashScope text-embedding-v3，1024维';

-- 余弦距离索引（数据少时也可先不建，顺序扫也够演示）
CREATE INDEX IF NOT EXISTS idx_goods_search_embedding_hnsw
    ON goods_search_embedding
    USING hnsw (embedding vector_cosine_ops);

-- 业务账号权限（按你实际用户名调整；默认 inventory_user）
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE goods_search_embedding TO inventory_user;
GRANT USAGE, SELECT ON SEQUENCE goods_search_embedding_id_seq TO inventory_user;
