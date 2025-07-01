CREATE TABLE project (
                         project_id UUID PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         created_at TIMESTAMP WITH TIME ZONE,
                         updated_at TIMESTAMP WITH TIME ZONE,
                         status VARCHAR(20),      -- expected values: 'ACTIVE', 'ARCHIVED'
                         visibility VARCHAR(20),  -- expected values: 'PRIVATE', 'PUBLIC'
                         project_owner_id UUID
);

CREATE TABLE ticket (
                        ticket_id UUID PRIMARY KEY,
                        subject VARCHAR(255) NOT NULL,
                        description TEXT,
                        status VARCHAR(50),
                        priority INTEGER,
                        created_at TIMESTAMP WITH TIME ZONE,
                        updated_at TIMESTAMP WITH TIME ZONE,
                        due_date DATE,
                        user_id UUID,       -- FK to user.user_id
                        assignee_id UUID,   -- FK to user.user_id
                        project_id UUID     -- FK to project.project_id
);

CREATE TABLE user_pls (
                      user_id UUID PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      email VARCHAR(255),
                      role VARCHAR(20),    -- expected values: 'ADMIN', 'AGENT', 'USER'
                      status VARCHAR(20),  -- expected values: 'ACTIVE', 'SUSPENDED'
                      timezone VARCHAR(100)
);

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES user_pls(user_id);

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES user_pls(user_id);

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_project FOREIGN KEY (project_id) REFERENCES project(project_id);


