-- shop_product 新增厂家、联系方式、备注（若库表已手动加列可跳过）

ALTER TABLE public.shop_product
    ADD COLUMN IF NOT EXISTS factory varchar(128) NULL,
    ADD COLUMN IF NOT EXISTS factory_contact varchar(64) NULL,
    ADD COLUMN IF NOT EXISTS remark varchar(512) NULL;

COMMENT ON COLUMN public.shop_product.factory IS '厂家';
COMMENT ON COLUMN public.shop_product.factory_contact IS '厂家联系方式';
COMMENT ON COLUMN public.shop_product.remark IS '备注信息';
