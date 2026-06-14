-- public.goods_brand 定义

-- Drop table

-- DROP TABLE public.goods_brand;

CREATE TABLE public.goods_brand (
	id int8 NOT NULL, -- 雪花ID
	brand_name varchar(100) NOT NULL, -- 品牌名称
	brand_code varchar(50) NULL, -- 品牌编码
	logo varchar(255) NULL, -- 品牌logo
	sort int4 DEFAULT 0 NOT NULL, -- 排序号
	status int4 DEFAULT 1 NOT NULL, -- 状态 1=启用 0=禁用
	remark varchar(500) NULL, -- 备注
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	is_deleted int4 DEFAULT 0 NOT NULL, -- 逻辑删除 0=未删 1=已删
	CONSTRAINT pk_goods_brand PRIMARY KEY (id)
);
CREATE INDEX idx_goods_brand_status ON public.goods_brand USING btree (status);
COMMENT ON TABLE public.goods_brand IS '商品品牌表';

-- Column comments

COMMENT ON COLUMN public.goods_brand.id IS '雪花ID';
COMMENT ON COLUMN public.goods_brand.brand_name IS '品牌名称';
COMMENT ON COLUMN public.goods_brand.brand_code IS '品牌编码';
COMMENT ON COLUMN public.goods_brand.logo IS '品牌logo';
COMMENT ON COLUMN public.goods_brand.sort IS '排序号';
COMMENT ON COLUMN public.goods_brand.status IS '状态 1=启用 0=禁用';
COMMENT ON COLUMN public.goods_brand.remark IS '备注';
COMMENT ON COLUMN public.goods_brand.create_time IS '创建时间';
COMMENT ON COLUMN public.goods_brand.update_time IS '更新时间';
COMMENT ON COLUMN public.goods_brand.is_deleted IS '逻辑删除 0=未删 1=已删';


-- public.goods_category 定义

-- Drop table

-- DROP TABLE public.goods_category;

CREATE TABLE public.goods_category (
	id int8 NOT NULL, -- 雪花ID
	parent_id int8 DEFAULT 0 NOT NULL, -- 上级分类ID
	category_name varchar(100) NOT NULL, -- 分类名称
	sort int4 DEFAULT 0 NOT NULL, -- 排序号
	status int4 DEFAULT 1 NOT NULL, -- 状态 1=启用 0=禁用
	remark varchar(500) NULL, -- 备注
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	is_deleted int4 DEFAULT 0 NOT NULL, -- 逻辑删除 0=未删 1=已删
	CONSTRAINT pk_goods_category PRIMARY KEY (id)
);
CREATE INDEX idx_goods_category_parent_id ON public.goods_category USING btree (parent_id);
CREATE INDEX idx_goods_category_status ON public.goods_category USING btree (status);
COMMENT ON TABLE public.goods_category IS '商品分类表';

-- Column comments

COMMENT ON COLUMN public.goods_category.id IS '雪花ID';
COMMENT ON COLUMN public.goods_category.parent_id IS '上级分类ID';
COMMENT ON COLUMN public.goods_category.category_name IS '分类名称';
COMMENT ON COLUMN public.goods_category.sort IS '排序号';
COMMENT ON COLUMN public.goods_category.status IS '状态 1=启用 0=禁用';
COMMENT ON COLUMN public.goods_category.remark IS '备注';
COMMENT ON COLUMN public.goods_category.create_time IS '创建时间';
COMMENT ON COLUMN public.goods_category.update_time IS '更新时间';
COMMENT ON COLUMN public.goods_category.is_deleted IS '逻辑删除 0=未删 1=已删';


-- public.goods_product 定义

-- Drop table

-- DROP TABLE public.goods_product;

CREATE TABLE public.goods_product (
	id int8 NOT NULL, -- 雪花算法ID
	main_image varchar(255) NULL, -- 主图URL
	images text NULL, -- 商品图片列表JSON数组
	product_name varchar(200) NOT NULL, -- 商品名称
	spec_model varchar(100) NULL, -- 规格型号
	product_code varchar(50) NULL, -- 商品编码
	category_id int8 NOT NULL, -- 分类ID
	category_name varchar(100) NULL, -- 分类名称
	brand_id int8 NULL, -- 品牌ID
	brand_name varchar(100) NULL, -- 品牌名称
	supplier_name varchar(100) NULL, -- 供应商
	manufacturer varchar(100) NULL, -- 厂家
	unit varchar(20) NULL, -- 单位
	cost_price numeric(10, 2) NULL, -- 进货价/成本价
	sale_price numeric(10, 2) NOT NULL, -- 售价(标价)
	stock int4 DEFAULT 0 NULL, -- 当前库存
	stock_warn int4 DEFAULT 0 NULL, -- 库存预警值
	showcase_position varchar(50) NULL, -- 橱窗位置
	shelf_status int4 NOT NULL, -- 上下架状态 1=上架 0=下架
	sort int4 DEFAULT 0 NULL, -- 排序号(数字越小越靠前)
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	is_deleted int4 DEFAULT 0 NULL, -- 逻辑删除 0=正常 1=已删除
	CONSTRAINT pk_goods_product PRIMARY KEY (id)
);
CREATE INDEX idx_goods_product_brand_id ON public.goods_product USING btree (brand_id);
CREATE INDEX idx_goods_product_category_id ON public.goods_product USING btree (category_id);
CREATE INDEX idx_goods_product_product_code ON public.goods_product USING btree (product_code);
CREATE INDEX idx_goods_product_shelf_status ON public.goods_product USING btree (shelf_status);
COMMENT ON TABLE public.goods_product IS '商品信息表';

-- Column comments

COMMENT ON COLUMN public.goods_product.id IS '雪花算法ID';
COMMENT ON COLUMN public.goods_product.main_image IS '主图URL';
COMMENT ON COLUMN public.goods_product.images IS '商品图片列表JSON数组';
COMMENT ON COLUMN public.goods_product.product_name IS '商品名称';
COMMENT ON COLUMN public.goods_product.spec_model IS '规格型号';
COMMENT ON COLUMN public.goods_product.product_code IS '商品编码';
COMMENT ON COLUMN public.goods_product.category_id IS '分类ID';
COMMENT ON COLUMN public.goods_product.category_name IS '分类名称';
COMMENT ON COLUMN public.goods_product.brand_id IS '品牌ID';
COMMENT ON COLUMN public.goods_product.brand_name IS '品牌名称';
COMMENT ON COLUMN public.goods_product.supplier_name IS '供应商';
COMMENT ON COLUMN public.goods_product.manufacturer IS '厂家';
COMMENT ON COLUMN public.goods_product.unit IS '单位';
COMMENT ON COLUMN public.goods_product.cost_price IS '进货价/成本价';
COMMENT ON COLUMN public.goods_product.sale_price IS '售价(标价)';
COMMENT ON COLUMN public.goods_product.stock IS '当前库存';
COMMENT ON COLUMN public.goods_product.stock_warn IS '库存预警值';
COMMENT ON COLUMN public.goods_product.showcase_position IS '橱窗位置';
COMMENT ON COLUMN public.goods_product.shelf_status IS '上下架状态 1=上架 0=下架';
COMMENT ON COLUMN public.goods_product.sort IS '排序号(数字越小越靠前)';
COMMENT ON COLUMN public.goods_product.create_time IS '创建时间';
COMMENT ON COLUMN public.goods_product.update_time IS '更新时间';
COMMENT ON COLUMN public.goods_product.is_deleted IS '逻辑删除 0=正常 1=已删除';


-- public.inventory_flow 定义

-- Drop table

-- DROP TABLE public.inventory_flow;

CREATE TABLE public.inventory_flow (
	id int8 NOT NULL, -- 流水主键
	goods_id int8 NOT NULL, -- 商品ID
	goods_name varchar(255) NOT NULL, -- 商品名称
	before_stock int4 DEFAULT 0 NOT NULL, -- 变动前库存
	change_stock int4 DEFAULT 0 NOT NULL, -- 变动数量（正增负减）
	after_stock int4 DEFAULT 0 NOT NULL, -- 变动后库存
	operate_type int4 NOT NULL, -- 操作类型：1-入库 2-出库 3-锁定 4-解锁 5-调整
	"operator" varchar(50) NULL, -- 操作人
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 操作时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	biz_no varchar(64) NULL, -- 业务单号
	remark varchar(500) NULL, -- 备注
	sort int4 DEFAULT 0 NOT NULL, -- 排序号，数字越小越靠前
	CONSTRAINT pk_inventory_transaction PRIMARY KEY (id)
);
CREATE INDEX idx_inventory_transaction_biz_no ON public.inventory_flow USING btree (biz_no);
CREATE INDEX idx_inventory_transaction_goods_id ON public.inventory_flow USING btree (goods_id);
CREATE INDEX idx_inventory_transaction_operate_type ON public.inventory_flow USING btree (operate_type);
COMMENT ON TABLE public.inventory_flow IS '库存流水表';

-- Column comments

COMMENT ON COLUMN public.inventory_flow.id IS '流水主键';
COMMENT ON COLUMN public.inventory_flow.goods_id IS '商品ID';
COMMENT ON COLUMN public.inventory_flow.goods_name IS '商品名称';
COMMENT ON COLUMN public.inventory_flow.before_stock IS '变动前库存';
COMMENT ON COLUMN public.inventory_flow.change_stock IS '变动数量（正增负减）';
COMMENT ON COLUMN public.inventory_flow.after_stock IS '变动后库存';
COMMENT ON COLUMN public.inventory_flow.operate_type IS '操作类型：1-入库 2-出库 3-锁定 4-解锁 5-调整';
COMMENT ON COLUMN public.inventory_flow."operator" IS '操作人';
COMMENT ON COLUMN public.inventory_flow.create_time IS '操作时间';
COMMENT ON COLUMN public.inventory_flow.update_time IS '更新时间';
COMMENT ON COLUMN public.inventory_flow.biz_no IS '业务单号';
COMMENT ON COLUMN public.inventory_flow.remark IS '备注';
COMMENT ON COLUMN public.inventory_flow.sort IS '排序号，数字越小越靠前';


-- public.inventory_in 定义

-- Drop table

-- DROP TABLE public.inventory_in;

CREATE TABLE public.inventory_in (
	id int8 NOT NULL, -- 入库单主键
	receipt_no varchar(64) NOT NULL, -- 入库单号
	goods_id int8 NOT NULL, -- 商品ID
	goods_name varchar(255) NOT NULL, -- 商品名称
	receipt_qty int4 DEFAULT 0 NOT NULL, -- 入库数量
	"operator" varchar(50) NULL, -- 操作人
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	remark varchar(500) NULL, -- 备注
	sort int4 DEFAULT 0 NOT NULL, -- 排序号，数字越小越靠前
	CONSTRAINT pk_inventory_in PRIMARY KEY (id)
);
CREATE INDEX idx_inventory_in_goods_id ON public.inventory_in USING btree (goods_id);
CREATE INDEX idx_inventory_in_receipt_no ON public.inventory_in USING btree (receipt_no);
COMMENT ON TABLE public.inventory_in IS '入库信息表';

-- Column comments

COMMENT ON COLUMN public.inventory_in.id IS '入库单主键';
COMMENT ON COLUMN public.inventory_in.receipt_no IS '入库单号';
COMMENT ON COLUMN public.inventory_in.goods_id IS '商品ID';
COMMENT ON COLUMN public.inventory_in.goods_name IS '商品名称';
COMMENT ON COLUMN public.inventory_in.receipt_qty IS '入库数量';
COMMENT ON COLUMN public.inventory_in."operator" IS '操作人';
COMMENT ON COLUMN public.inventory_in.create_time IS '创建时间';
COMMENT ON COLUMN public.inventory_in.update_time IS '更新时间';
COMMENT ON COLUMN public.inventory_in.remark IS '备注';
COMMENT ON COLUMN public.inventory_in.sort IS '排序号，数字越小越靠前';


-- public.inventory_out 定义

-- Drop table

-- DROP TABLE public.inventory_out;

CREATE TABLE public.inventory_out (
	id int8 NOT NULL, -- 出库单主键
	outbound_no varchar(64) NOT NULL, -- 出库单号
	goods_id int8 NOT NULL, -- 商品ID
	goods_name varchar(255) NOT NULL, -- 商品名称
	outbound_qty int4 DEFAULT 0 NOT NULL, -- 出库数量
	"operator" varchar(50) NULL, -- 操作人
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	remark varchar(500) NULL, -- 备注
	sort int4 DEFAULT 0 NOT NULL, -- 排序号，数字越小越靠前
	CONSTRAINT pk_inventory_out PRIMARY KEY (id)
);
CREATE INDEX idx_inventory_out_goods_id ON public.inventory_out USING btree (goods_id);
CREATE INDEX idx_inventory_out_outbound_no ON public.inventory_out USING btree (outbound_no);
COMMENT ON TABLE public.inventory_out IS '出库信息表';

-- Column comments

COMMENT ON COLUMN public.inventory_out.id IS '出库单主键';
COMMENT ON COLUMN public.inventory_out.outbound_no IS '出库单号';
COMMENT ON COLUMN public.inventory_out.goods_id IS '商品ID';
COMMENT ON COLUMN public.inventory_out.goods_name IS '商品名称';
COMMENT ON COLUMN public.inventory_out.outbound_qty IS '出库数量';
COMMENT ON COLUMN public.inventory_out."operator" IS '操作人';
COMMENT ON COLUMN public.inventory_out.create_time IS '创建时间';
COMMENT ON COLUMN public.inventory_out.update_time IS '更新时间';
COMMENT ON COLUMN public.inventory_out.remark IS '备注';
COMMENT ON COLUMN public.inventory_out.sort IS '排序号，数字越小越靠前';


-- public.inventory_stock 定义

-- Drop table

-- DROP TABLE public.inventory_stock;

CREATE TABLE public.inventory_stock (
	id int8 NOT NULL, -- 库存记录主键
	goods_id int8 NOT NULL, -- 商品ID
	goods_name varchar(255) NOT NULL, -- 商品名称
	category_name varchar(100) NULL, -- 分类名称
	stock int4 DEFAULT 0 NOT NULL, -- 库存数量
	lock_stock int4 DEFAULT 0 NOT NULL, -- 锁定库存
	stock_warn int4 DEFAULT 0 NOT NULL, -- 库存预警值
	stock_status int4 DEFAULT 1 NOT NULL, -- 库存状态：1-正常 2-预警 3-缺货
	sort int4 DEFAULT 0 NOT NULL, -- 排序号，数字越小越靠前
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	last_receipt_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 最近更新时间
	operate_type int4 NULL, -- 操作：1-入库 2-出库
	CONSTRAINT pk_inventory_stock PRIMARY KEY (id)
);
CREATE INDEX idx_inventory_stock_goods_id ON public.inventory_stock USING btree (goods_id);
CREATE INDEX idx_inventory_stock_status ON public.inventory_stock USING btree (stock_status);
COMMENT ON TABLE public.inventory_stock IS '库存信息表';

-- Column comments

COMMENT ON COLUMN public.inventory_stock.id IS '库存记录主键';
COMMENT ON COLUMN public.inventory_stock.goods_id IS '商品ID';
COMMENT ON COLUMN public.inventory_stock.goods_name IS '商品名称';
COMMENT ON COLUMN public.inventory_stock.category_name IS '分类名称';
COMMENT ON COLUMN public.inventory_stock.stock IS '库存数量';
COMMENT ON COLUMN public.inventory_stock.lock_stock IS '锁定库存';
COMMENT ON COLUMN public.inventory_stock.stock_warn IS '库存预警值';
COMMENT ON COLUMN public.inventory_stock.stock_status IS '库存状态：1-正常 2-预警 3-缺货';
COMMENT ON COLUMN public.inventory_stock.sort IS '排序号，数字越小越靠前';
COMMENT ON COLUMN public.inventory_stock.create_time IS '创建时间';
COMMENT ON COLUMN public.inventory_stock.update_time IS '更新时间';
COMMENT ON COLUMN public.inventory_stock.last_receipt_time IS '最近更新时间';
COMMENT ON COLUMN public.inventory_stock.operate_type IS '操作：1-入库 2-出库';


-- public.order_delivery 定义

-- Drop table

-- DROP TABLE public.order_delivery;

CREATE TABLE public.order_delivery (
	id int8 NOT NULL, -- 订单主键
	order_no varchar(64) NOT NULL, -- 订单号
	user_id int8 NULL, -- 用户ID
	user_name varchar(100) NULL, -- 用户名称
	goods_id int8 NULL, -- 商品ID
	goods_name varchar(255) NOT NULL, -- 商品名称
	buy_qty int4 DEFAULT 1 NOT NULL, -- 购买数量
	order_amount numeric(10, 2) NOT NULL, -- 订单金额
	order_status int4 NOT NULL, -- 订单状态：1-待发货 2-已发货 3-已收货 4-退款中 5-退款完成
	logistics_no varchar(100) NULL, -- 物流单号
	remark varchar(500) NULL, -- 备注
	sort int4 DEFAULT 0 NOT NULL, -- 排序号，数字越小越靠前
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	CONSTRAINT pk_order_delivery PRIMARY KEY (id)
);
CREATE INDEX idx_order_delivery_goods_id ON public.order_delivery USING btree (goods_id);
CREATE INDEX idx_order_delivery_order_no ON public.order_delivery USING btree (order_no);
CREATE INDEX idx_order_delivery_order_status ON public.order_delivery USING btree (order_status);
COMMENT ON TABLE public.order_delivery IS '订单发货表';

-- Column comments

COMMENT ON COLUMN public.order_delivery.id IS '订单主键';
COMMENT ON COLUMN public.order_delivery.order_no IS '订单号';
COMMENT ON COLUMN public.order_delivery.user_id IS '用户ID';
COMMENT ON COLUMN public.order_delivery.user_name IS '用户名称';
COMMENT ON COLUMN public.order_delivery.goods_id IS '商品ID';
COMMENT ON COLUMN public.order_delivery.goods_name IS '商品名称';
COMMENT ON COLUMN public.order_delivery.buy_qty IS '购买数量';
COMMENT ON COLUMN public.order_delivery.order_amount IS '订单金额';
COMMENT ON COLUMN public.order_delivery.order_status IS '订单状态：1-待发货 2-已发货 3-已收货 4-退款中 5-退款完成';
COMMENT ON COLUMN public.order_delivery.logistics_no IS '物流单号';
COMMENT ON COLUMN public.order_delivery.remark IS '备注';
COMMENT ON COLUMN public.order_delivery.sort IS '排序号，数字越小越靠前';
COMMENT ON COLUMN public.order_delivery.create_time IS '创建时间';
COMMENT ON COLUMN public.order_delivery.update_time IS '更新时间';


-- public.order_info 定义

-- Drop table

-- DROP TABLE public.order_info;

CREATE TABLE public.order_info (
	id int8 NOT NULL, -- 订单主键
	order_no varchar(64) NOT NULL, -- 订单号
	user_id int8 NULL, -- 用户ID
	user_name varchar(100) NULL, -- 下单用户
	goods_id int8 NULL, -- 商品ID
	goods_name varchar(255) NOT NULL, -- 商品名称
	cost_price numeric(10, 2) NULL, -- 进货价/成本价
	sale_price numeric(10, 2) NOT NULL, -- 售价(标价)
	buy_qty int4 DEFAULT 1 NOT NULL, -- 购买数量
	order_amount numeric(10, 2) NOT NULL, -- 订单金额
	order_status int4 DEFAULT 0 NOT NULL, -- 订单状态：0-待支付 1-待发货 2-已发货 3-已收货 4-退款中 5-退款完成 6-已取消
	pay_time timestamp NULL, -- 支付时间
	logistics_no varchar(100) NULL, -- 物流单号
	remark varchar(500) NULL, -- 备注
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	receive_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 收货时间
	CONSTRAINT pk_order_info PRIMARY KEY (id)
);
CREATE INDEX idx_order_info_goods_id ON public.order_info USING btree (goods_id);
CREATE INDEX idx_order_info_order_no ON public.order_info USING btree (order_no);
CREATE INDEX idx_order_info_order_status ON public.order_info USING btree (order_status);
COMMENT ON TABLE public.order_info IS '订单信息表';

-- Column comments

COMMENT ON COLUMN public.order_info.id IS '订单主键';
COMMENT ON COLUMN public.order_info.order_no IS '订单号';
COMMENT ON COLUMN public.order_info.user_id IS '用户ID';
COMMENT ON COLUMN public.order_info.user_name IS '下单用户';
COMMENT ON COLUMN public.order_info.goods_id IS '商品ID';
COMMENT ON COLUMN public.order_info.goods_name IS '商品名称';
COMMENT ON COLUMN public.order_info.cost_price IS '进货价/成本价';
COMMENT ON COLUMN public.order_info.sale_price IS '售价(标价)';
COMMENT ON COLUMN public.order_info.buy_qty IS '购买数量';
COMMENT ON COLUMN public.order_info.order_amount IS '订单金额';
COMMENT ON COLUMN public.order_info.order_status IS '订单状态：0-待支付 1-待发货 2-已发货 3-已收货 4-退款中 5-退款完成 6-已取消';
COMMENT ON COLUMN public.order_info.pay_time IS '支付时间';
COMMENT ON COLUMN public.order_info.logistics_no IS '物流单号';
COMMENT ON COLUMN public.order_info.remark IS '备注';
COMMENT ON COLUMN public.order_info.create_time IS '创建时间';
COMMENT ON COLUMN public.order_info.update_time IS '更新时间';
COMMENT ON COLUMN public.order_info.receive_time IS '收货时间';


-- public.order_refund 定义

-- Drop table

-- DROP TABLE public.order_refund;

CREATE TABLE public.order_refund (
	id int8 NOT NULL, -- 退款单主键
	order_id int8 NULL, -- 关联订单ID
	order_no varchar(64) NOT NULL, -- 订单号
	user_name varchar(100) NULL, -- 用户名称
	goods_name varchar(255) NOT NULL, -- 商品名称
	refund_amount numeric(10, 2) NOT NULL, -- 退款金额
	refund_status int4 DEFAULT 0 NOT NULL, -- 退款状态：0-待审核 1-审核通过 2-已拒绝
	apply_time timestamp NULL, -- 申请时间
	audit_remark varchar(500) NULL, -- 审核备注
	sort int4 DEFAULT 0 NOT NULL, -- 排序号，数字越小越靠前
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	original_delivery_status int4 NULL, -- 发起退款时的状态值
	original_order_status int4 NULL, -- 发起退款时的状态值
	CONSTRAINT pk_order_refund PRIMARY KEY (id)
);
CREATE INDEX idx_order_refund_order_id ON public.order_refund USING btree (order_id);
CREATE INDEX idx_order_refund_order_no ON public.order_refund USING btree (order_no);
CREATE INDEX idx_order_refund_refund_status ON public.order_refund USING btree (refund_status);
COMMENT ON TABLE public.order_refund IS '订单退款表';

-- Column comments

COMMENT ON COLUMN public.order_refund.id IS '退款单主键';
COMMENT ON COLUMN public.order_refund.order_id IS '关联订单ID';
COMMENT ON COLUMN public.order_refund.order_no IS '订单号';
COMMENT ON COLUMN public.order_refund.user_name IS '用户名称';
COMMENT ON COLUMN public.order_refund.goods_name IS '商品名称';
COMMENT ON COLUMN public.order_refund.refund_amount IS '退款金额';
COMMENT ON COLUMN public.order_refund.refund_status IS '退款状态：0-待审核 1-审核通过 2-已拒绝';
COMMENT ON COLUMN public.order_refund.apply_time IS '申请时间';
COMMENT ON COLUMN public.order_refund.audit_remark IS '审核备注';
COMMENT ON COLUMN public.order_refund.sort IS '排序号，数字越小越靠前';
COMMENT ON COLUMN public.order_refund.create_time IS '创建时间';
COMMENT ON COLUMN public.order_refund.update_time IS '更新时间';
COMMENT ON COLUMN public.order_refund.original_delivery_status IS '发起退款时的状态值';
COMMENT ON COLUMN public.order_refund.original_order_status IS '发起退款时的状态值';


-- public.shop_product 定义

-- Drop table

-- DROP TABLE public.shop_product;

CREATE TABLE public.shop_product (
	id int8 NOT NULL, -- 雪花主键ID
	product_name varchar(128) NOT NULL, -- 商品配件名称
	cost_price numeric(10, 2) NOT NULL, -- 进货单价
	sale_price numeric(10, 2) NOT NULL, -- 售卖单价
	stock int4 DEFAULT 0 NOT NULL, -- 当前库存
	factory varchar(128) NULL, -- 厂家
	factory_contact varchar(64) NULL, -- 厂家联系方式
	remark varchar(512) NULL, -- 备注信息
	stock_warn int4 NULL, -- 库存预警值
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	is_deleted int4 DEFAULT 0 NOT NULL, -- 逻辑删除 0=正常 1=已删除
	CONSTRAINT shop_product_cost_price_check CHECK ((cost_price >= (0)::numeric)),
	CONSTRAINT shop_product_pkey PRIMARY KEY (id),
	CONSTRAINT shop_product_sale_price_check CHECK ((sale_price >= (0)::numeric)),
	CONSTRAINT shop_product_stock_check CHECK ((stock >= 0)),
	CONSTRAINT shop_product_stock_warn_check CHECK ((stock_warn >= 0))
);
CREATE INDEX idx_shop_product_name ON public.shop_product USING btree (product_name);
COMMENT ON TABLE public.shop_product IS '商品配件信息表';

-- Column comments

COMMENT ON COLUMN public.shop_product.id IS '雪花主键ID';
COMMENT ON COLUMN public.shop_product.product_name IS '商品配件名称';
COMMENT ON COLUMN public.shop_product.cost_price IS '进货单价';
COMMENT ON COLUMN public.shop_product.sale_price IS '售卖单价';
COMMENT ON COLUMN public.shop_product.stock IS '当前库存';
COMMENT ON COLUMN public.shop_product.factory IS '厂家';
COMMENT ON COLUMN public.shop_product.factory_contact IS '厂家联系方式';
COMMENT ON COLUMN public.shop_product.remark IS '备注信息';
COMMENT ON COLUMN public.shop_product.stock_warn IS '库存预警值';
COMMENT ON COLUMN public.shop_product.create_time IS '创建时间';
COMMENT ON COLUMN public.shop_product.update_time IS '更新时间';
COMMENT ON COLUMN public.shop_product.is_deleted IS '逻辑删除 0=正常 1=已删除';


-- public.shop_sale_record 定义

-- Drop table

-- DROP TABLE public.shop_sale_record;

CREATE TABLE public.shop_sale_record (
	id int8 NOT NULL, -- 雪花主键ID
	product_id int8 NOT NULL, -- 关联商品ID
	product_name varchar(128) NOT NULL, -- 商品名称(冗余)
	sale_price numeric(10, 2) NOT NULL, -- 本次成交售价(支持改价)
	quantity int4 NOT NULL, -- 购买数量
	total_amount numeric(10, 2) NOT NULL, -- 实收总金额
	profit numeric(10, 2) NULL, -- 单笔利润
	sale_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 售卖时间
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 单据创建时间
	is_deleted int4 DEFAULT 0 NOT NULL, -- 逻辑删除 0=正常 1=已删除
	CONSTRAINT shop_sale_record_pkey PRIMARY KEY (id),
	CONSTRAINT shop_sale_record_quantity_check CHECK ((quantity >= 1)),
	CONSTRAINT shop_sale_record_sale_price_check CHECK ((sale_price >= (0)::numeric)),
	CONSTRAINT shop_sale_record_total_amount_check CHECK ((total_amount >= (0)::numeric))
);
CREATE INDEX idx_shop_sale_name ON public.shop_sale_record USING btree (product_name);
CREATE INDEX idx_shop_sale_time ON public.shop_sale_record USING btree (sale_time);
COMMENT ON TABLE public.shop_sale_record IS '销售流水记录表';

-- Column comments

COMMENT ON COLUMN public.shop_sale_record.id IS '雪花主键ID';
COMMENT ON COLUMN public.shop_sale_record.product_id IS '关联商品ID';
COMMENT ON COLUMN public.shop_sale_record.product_name IS '商品名称(冗余)';
COMMENT ON COLUMN public.shop_sale_record.sale_price IS '本次成交售价(支持改价)';
COMMENT ON COLUMN public.shop_sale_record.quantity IS '购买数量';
COMMENT ON COLUMN public.shop_sale_record.total_amount IS '实收总金额';
COMMENT ON COLUMN public.shop_sale_record.profit IS '单笔利润';
COMMENT ON COLUMN public.shop_sale_record.sale_time IS '售卖时间';
COMMENT ON COLUMN public.shop_sale_record.create_time IS '单据创建时间';
COMMENT ON COLUMN public.shop_sale_record.is_deleted IS '逻辑删除 0=正常 1=已删除';


-- public.sys_api_monitor 定义

-- Drop table

-- DROP TABLE public.sys_api_monitor;

CREATE TABLE public.sys_api_monitor (
	id int8 NOT NULL, -- 主键ID(雪花ID)
	api_path varchar(512) NOT NULL, -- 接口路径
	request_method varchar(32) NULL, -- 请求方式
	response_time int8 NULL, -- 响应耗时(ms)
	success_flag int4 NULL, -- 是否成功 0-失败 1-成功
	create_time timestamp NULL, -- 创建时间
	CONSTRAINT sys_api_monitor_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_sys_api_monitor_api_path ON public.sys_api_monitor USING btree (api_path);
CREATE INDEX idx_sys_api_monitor_create_time ON public.sys_api_monitor USING btree (create_time);
COMMENT ON TABLE public.sys_api_monitor IS '接口监控记录表';

-- Column comments

COMMENT ON COLUMN public.sys_api_monitor.id IS '主键ID(雪花ID)';
COMMENT ON COLUMN public.sys_api_monitor.api_path IS '接口路径';
COMMENT ON COLUMN public.sys_api_monitor.request_method IS '请求方式';
COMMENT ON COLUMN public.sys_api_monitor.response_time IS '响应耗时(ms)';
COMMENT ON COLUMN public.sys_api_monitor.success_flag IS '是否成功 0-失败 1-成功';
COMMENT ON COLUMN public.sys_api_monitor.create_time IS '创建时间';


-- public.sys_login_log 定义

-- Drop table

-- DROP TABLE public.sys_login_log;

CREATE TABLE public.sys_login_log (
	id int8 NOT NULL, -- 主键ID
	user_id int8 NULL, -- 用户ID
	user_name varchar(50) NULL, -- 登录账号
	nick_name varchar(50) NULL, -- 用户昵称
	login_ip varchar(128) NULL, -- 登录IP
	login_address varchar(255) NULL, -- 登录地点
	browser varchar(50) NULL, -- 浏览器
	operating_system varchar(50) NULL, -- 操作系统
	login_status int2 NULL, -- 登录状态
	fail_reason varchar(255) NULL, -- 失败原因
	user_agent text NULL, -- 客户端标识
	login_time timestamp(6) NULL, -- 登录时间
	created_time timestamp(6) NULL, -- 创建时间
	CONSTRAINT pk_sys_login_log PRIMARY KEY (id)
);
CREATE INDEX idx_sys_login_log_login_ip ON public.sys_login_log USING btree (login_ip);
CREATE INDEX idx_sys_login_log_login_status ON public.sys_login_log USING btree (login_status);
CREATE INDEX idx_sys_login_log_login_time ON public.sys_login_log USING btree (login_time DESC);
CREATE INDEX idx_sys_login_log_user_id ON public.sys_login_log USING btree (user_id);
COMMENT ON TABLE public.sys_login_log IS '登录日志表';

-- Column comments

COMMENT ON COLUMN public.sys_login_log.id IS '主键ID';
COMMENT ON COLUMN public.sys_login_log.user_id IS '用户ID';
COMMENT ON COLUMN public.sys_login_log.user_name IS '登录账号';
COMMENT ON COLUMN public.sys_login_log.nick_name IS '用户昵称';
COMMENT ON COLUMN public.sys_login_log.login_ip IS '登录IP';
COMMENT ON COLUMN public.sys_login_log.login_address IS '登录地点';
COMMENT ON COLUMN public.sys_login_log.browser IS '浏览器';
COMMENT ON COLUMN public.sys_login_log.operating_system IS '操作系统';
COMMENT ON COLUMN public.sys_login_log.login_status IS '登录状态';
COMMENT ON COLUMN public.sys_login_log.fail_reason IS '失败原因';
COMMENT ON COLUMN public.sys_login_log.user_agent IS '客户端标识';
COMMENT ON COLUMN public.sys_login_log.login_time IS '登录时间';
COMMENT ON COLUMN public.sys_login_log.created_time IS '创建时间';


-- public.sys_operation_log 定义

-- Drop table

-- DROP TABLE public.sys_operation_log;

CREATE TABLE public.sys_operation_log (
	id bigserial NOT NULL, -- 主键ID
	username varchar(64) DEFAULT ''::character varying NOT NULL, -- 操作人账号
	title varchar(128) DEFAULT ''::character varying NOT NULL, -- 操作模块
	operation_type varchar(32) DEFAULT ''::character varying NOT NULL, -- 操作类型
	request_method varchar(16) DEFAULT ''::character varying NOT NULL, -- 请求方法
	request_uri varchar(256) DEFAULT ''::character varying NOT NULL, -- 请求接口
	ip_address varchar(64) DEFAULT ''::character varying NOT NULL, -- 客户端IP
	browser varchar(128) DEFAULT ''::character varying NOT NULL, -- 浏览器
	operating_system varchar(128) DEFAULT ''::character varying NOT NULL, -- 操作系统
	request_params text NULL, -- 请求参数
	operate_status int2 DEFAULT 1 NOT NULL, -- 状态 1成功 0失败
	error_message text NULL, -- 异常信息
	create_time timestamp(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL, -- 创建时间
	update_time timestamp(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
	CONSTRAINT sys_operation_log_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_operation_log_create_time ON public.sys_operation_log USING btree (create_time);
CREATE INDEX idx_operation_log_operate_status ON public.sys_operation_log USING btree (operate_status);
CREATE INDEX idx_operation_log_username ON public.sys_operation_log USING btree (username);
COMMENT ON TABLE public.sys_operation_log IS '系统操作审计日志表';

-- Column comments

COMMENT ON COLUMN public.sys_operation_log.id IS '主键ID';
COMMENT ON COLUMN public.sys_operation_log.username IS '操作人账号';
COMMENT ON COLUMN public.sys_operation_log.title IS '操作模块';
COMMENT ON COLUMN public.sys_operation_log.operation_type IS '操作类型';
COMMENT ON COLUMN public.sys_operation_log.request_method IS '请求方法';
COMMENT ON COLUMN public.sys_operation_log.request_uri IS '请求接口';
COMMENT ON COLUMN public.sys_operation_log.ip_address IS '客户端IP';
COMMENT ON COLUMN public.sys_operation_log.browser IS '浏览器';
COMMENT ON COLUMN public.sys_operation_log.operating_system IS '操作系统';
COMMENT ON COLUMN public.sys_operation_log.request_params IS '请求参数';
COMMENT ON COLUMN public.sys_operation_log.operate_status IS '状态 1成功 0失败';
COMMENT ON COLUMN public.sys_operation_log.error_message IS '异常信息';
COMMENT ON COLUMN public.sys_operation_log.create_time IS '创建时间';


-- public.sys_permission 定义

-- Drop table

-- DROP TABLE public.sys_permission;

CREATE TABLE public.sys_permission (
	id int8 NOT NULL, -- 主键ID（雪花算法）
	parent_id int8 DEFAULT 0 NULL, -- 父级权限ID（0=顶级）
	perm_name varchar(50) NOT NULL, -- 权限名称
	perm_type bpchar(1) NOT NULL, -- 权限类型：M=目录 C=菜单 F=按钮
	"path" varchar(255) NULL, -- 路由地址
	component varchar(255) NULL, -- 前端组件路径
	perm_code varchar(100) NULL, -- 权限标识，如 sys:user:list
	icon varchar(100) NULL, -- 菜单图标
	sort int4 DEFAULT 0 NULL, -- 显示排序
	status int2 DEFAULT 1 NULL, -- 状态 1=正常 0=禁用
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 更新时间
	is_deleted int2 DEFAULT 0 NULL, -- 删除标识 0=未删除 1=已删除
	CONSTRAINT sys_permission_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_permission IS '系统权限表';

-- Column comments

COMMENT ON COLUMN public.sys_permission.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN public.sys_permission.parent_id IS '父级权限ID（0=顶级）';
COMMENT ON COLUMN public.sys_permission.perm_name IS '权限名称';
COMMENT ON COLUMN public.sys_permission.perm_type IS '权限类型：M=目录 C=菜单 F=按钮';
COMMENT ON COLUMN public.sys_permission."path" IS '路由地址';
COMMENT ON COLUMN public.sys_permission.component IS '前端组件路径';
COMMENT ON COLUMN public.sys_permission.perm_code IS '权限标识，如 sys:user:list';
COMMENT ON COLUMN public.sys_permission.icon IS '菜单图标';
COMMENT ON COLUMN public.sys_permission.sort IS '显示排序';
COMMENT ON COLUMN public.sys_permission.status IS '状态 1=正常 0=禁用';
COMMENT ON COLUMN public.sys_permission.create_time IS '创建时间';
COMMENT ON COLUMN public.sys_permission.update_time IS '更新时间';
COMMENT ON COLUMN public.sys_permission.is_deleted IS '删除标识 0=未删除 1=已删除';


-- public.sys_role 定义

-- Drop table

-- DROP TABLE public.sys_role;

CREATE TABLE public.sys_role (
	id int8 NOT NULL, -- 主键ID
	role_name varchar(50) NOT NULL, -- 角色名称
	role_code varchar(100) NOT NULL, -- 角色编码（唯一）
	sort int4 DEFAULT 0 NULL, -- 显示排序
	status int2 DEFAULT 1 NULL, -- 状态 1=正常 0=禁用
	remark varchar(500) NULL, -- 备注说明
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 更新时间
	is_deleted int2 DEFAULT 0 NULL, -- 删除标识 0=未删除 1=已删除
	CONSTRAINT sys_role_pkey PRIMARY KEY (id),
	CONSTRAINT sys_role_role_code_key UNIQUE (role_code),
	CONSTRAINT uk_role_code UNIQUE (role_code)
);
COMMENT ON TABLE public.sys_role IS '系统角色表';

-- Column comments

COMMENT ON COLUMN public.sys_role.id IS '主键ID';
COMMENT ON COLUMN public.sys_role.role_name IS '角色名称';
COMMENT ON COLUMN public.sys_role.role_code IS '角色编码（唯一）';
COMMENT ON COLUMN public.sys_role.sort IS '显示排序';
COMMENT ON COLUMN public.sys_role.status IS '状态 1=正常 0=禁用';
COMMENT ON COLUMN public.sys_role.remark IS '备注说明';
COMMENT ON COLUMN public.sys_role.create_time IS '创建时间';
COMMENT ON COLUMN public.sys_role.update_time IS '更新时间';
COMMENT ON COLUMN public.sys_role.is_deleted IS '删除标识 0=未删除 1=已删除';


-- public.sys_role_permission 定义

-- Drop table

-- DROP TABLE public.sys_role_permission;

CREATE TABLE public.sys_role_permission (
	id int8 NOT NULL,
	role_id int8 NOT NULL, -- 角色ID
	perm_id int8 NOT NULL, -- 权限ID
	CONSTRAINT sys_role_permission_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_role_permission IS '角色权限关联表';

-- Column comments

COMMENT ON COLUMN public.sys_role_permission.role_id IS '角色ID';
COMMENT ON COLUMN public.sys_role_permission.perm_id IS '权限ID';


-- public.sys_user 定义

-- Drop table

-- DROP TABLE public.sys_user;

CREATE TABLE public.sys_user (
	id int8 NOT NULL, -- 主键ID
	user_name varchar(50) NOT NULL, -- 登录用户名
	"password" varchar(100) NOT NULL, -- 登录密码
	nick_name varchar(50) NULL, -- 用户昵称
	phone varchar(20) NULL, -- 手机号码
	email varchar(100) NULL, -- 邮箱地址
	avatar varchar(255) NULL, -- 头像路径
	sex int4 DEFAULT 0 NULL, -- 性别：0-未知 1-男 2-女
	age int4 NULL, -- 年龄
	status int4 DEFAULT 1 NOT NULL, -- 状态：1-正常 0-禁用
	sort int4 DEFAULT 0 NULL, -- 排序号
	remark varchar(500) NULL, -- 备注信息
	is_deleted int4 DEFAULT 0 NOT NULL, -- 逻辑删除标识
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	CONSTRAINT sys_user_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sys_user_user_name ON public.sys_user USING btree (user_name);
COMMENT ON TABLE public.sys_user IS '系统用户表';

-- Column comments

COMMENT ON COLUMN public.sys_user.id IS '主键ID';
COMMENT ON COLUMN public.sys_user.user_name IS '登录用户名';
COMMENT ON COLUMN public.sys_user."password" IS '登录密码';
COMMENT ON COLUMN public.sys_user.nick_name IS '用户昵称';
COMMENT ON COLUMN public.sys_user.phone IS '手机号码';
COMMENT ON COLUMN public.sys_user.email IS '邮箱地址';
COMMENT ON COLUMN public.sys_user.avatar IS '头像路径';
COMMENT ON COLUMN public.sys_user.sex IS '性别：0-未知 1-男 2-女';
COMMENT ON COLUMN public.sys_user.age IS '年龄';
COMMENT ON COLUMN public.sys_user.status IS '状态：1-正常 0-禁用';
COMMENT ON COLUMN public.sys_user.sort IS '排序号';
COMMENT ON COLUMN public.sys_user.remark IS '备注信息';
COMMENT ON COLUMN public.sys_user.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN public.sys_user.create_time IS '创建时间';
COMMENT ON COLUMN public.sys_user.update_time IS '更新时间';

-- Table Triggers

create trigger trigger_sys_user_update_time before
update
    on
    public.sys_user for each row execute function update_modified_column();


-- public.sys_user_role 定义

-- Drop table

-- DROP TABLE public.sys_user_role;

CREATE TABLE public.sys_user_role (
	id int8 NOT NULL,
	user_id int8 NOT NULL, -- 用户ID
	role_id int8 NOT NULL, -- 角色ID
	CONSTRAINT sys_user_role_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_user_role IS '用户角色关联表';

-- Column comments

COMMENT ON COLUMN public.sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN public.sys_user_role.role_id IS '角色ID';