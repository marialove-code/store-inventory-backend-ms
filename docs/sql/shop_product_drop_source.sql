-- 移除 shop_product.source（商品来源）字段

ALTER TABLE public.shop_product DROP COLUMN IF EXISTS "source";
