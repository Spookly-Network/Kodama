ALTER TABLE blueprint_port_definitions
    ADD CONSTRAINT uq_blueprint_port_definitions_blueprint_name
        UNIQUE (blueprint_id, name);
