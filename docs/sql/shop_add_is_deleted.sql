-- shop_product / shop_sale_record 新增逻辑删除字段（若库表已手动加列可跳过）

ALTER TABLE public.shop_product
    ADD COLUMN IF NOT EXISTS is_deleted int4 DEFAULT 0 NOT NULL;

COMMENT ON COLUMN public.shop_product.is_deleted IS '逻辑删除 0=正常 1=已删除';

ALTER TABLE public.shop_sale_record
    ADD COLUMN IF NOT EXISTS is_deleted int4 DEFAULT 0 NOT NULL;

COMMENT ON COLUMN public.shop_sale_record.is_deleted IS '逻辑删除 0=正常 1=已删除';
