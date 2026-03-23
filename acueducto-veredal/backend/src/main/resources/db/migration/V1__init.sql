CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE owner (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_owner_tenant_document UNIQUE (tenant_id, document_type, document_number),
    CONSTRAINT chk_owner_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE property (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    property_code VARCHAR(50) NOT NULL,
    property_name VARCHAR(200) NOT NULL,
    address_line VARCHAR(250) NOT NULL,
    owner_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_property_tenant_code UNIQUE (tenant_id, property_code),
    CONSTRAINT fk_property_owner FOREIGN KEY (owner_id) REFERENCES owner(id),
    CONSTRAINT chk_property_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE water_meter (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    meter_serial VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    installed_at DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_water_meter_tenant_serial UNIQUE (tenant_id, meter_serial),
    CONSTRAINT chk_water_meter_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DAMAGED', 'REMOVED'))
);

CREATE TABLE water_right (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    right_code VARCHAR(50) NOT NULL,
    owner_id UUID NOT NULL,
    property_id UUID NOT NULL,
    water_meter_id UUID,
    service_status VARCHAR(20) NOT NULL,
    connection_type VARCHAR(30) NOT NULL,
    base_tariff NUMERIC(14,2) NOT NULL,
    ownership_start_date DATE NOT NULL,
    ownership_end_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_water_right_tenant_code UNIQUE (tenant_id, right_code),
    CONSTRAINT fk_water_right_owner FOREIGN KEY (owner_id) REFERENCES owner(id),
    CONSTRAINT fk_water_right_property FOREIGN KEY (property_id) REFERENCES property(id),
    CONSTRAINT fk_water_right_meter FOREIGN KEY (water_meter_id) REFERENCES water_meter(id),
    CONSTRAINT chk_water_right_service_status CHECK (service_status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE')),
    CONSTRAINT chk_water_right_dates CHECK (ownership_end_date IS NULL OR ownership_end_date >= ownership_start_date),
    CONSTRAINT chk_water_right_tariff CHECK (base_tariff >= 0)
);

CREATE TABLE water_right_owner_history (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    water_right_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    transfer_reason VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_wr_owner_history_wr FOREIGN KEY (water_right_id) REFERENCES water_right(id),
    CONSTRAINT fk_wr_owner_history_owner FOREIGN KEY (owner_id) REFERENCES owner(id),
    CONSTRAINT chk_wr_owner_history_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE property_owner_history (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    property_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    change_reason VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_property_owner_history_property FOREIGN KEY (property_id) REFERENCES property(id),
    CONSTRAINT fk_property_owner_history_owner FOREIGN KEY (owner_id) REFERENCES owner(id),
    CONSTRAINT chk_property_owner_history_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_owner_tenant ON owner (tenant_id);
CREATE INDEX idx_property_tenant ON property (tenant_id);
CREATE INDEX idx_property_owner_tenant ON property (tenant_id, owner_id);
CREATE INDEX idx_water_meter_tenant ON water_meter (tenant_id);
CREATE INDEX idx_water_right_tenant ON water_right (tenant_id);
CREATE INDEX idx_water_right_owner_tenant ON water_right (tenant_id, owner_id);
CREATE INDEX idx_water_right_property_tenant ON water_right (tenant_id, property_id);
CREATE INDEX idx_wr_owner_history_tenant_wr ON water_right_owner_history (tenant_id, water_right_id, start_date DESC);
CREATE INDEX idx_property_owner_history_tenant_property ON property_owner_history (tenant_id, property_id, start_date DESC);
