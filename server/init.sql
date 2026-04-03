CREATE TABLE accounts (
    "username" VARCHAR(255) PRIMARY KEY,
    "name" VARCHAR(255),
    "experiencePoints" INTEGER NOT NULL,
    "experiencePointsToday" INTEGER NOT NULL
    );

CREATE TABLE groups (
    "groupID" BIGINT,
    "user" VARCHAR(255),
    "name" VARCHAR(255) NOT NULL,
    "colour" INTEGER NOT NULL,
    PRIMARY KEY ("groupID", "user"),
    CONSTRAINT groups_user_fkey FOREIGN KEY ("user")
    REFERENCES accounts("username")
    ON DELETE CASCADE
    );

CREATE TABLE events (
    "eventID" BIGINT,
    "eventType" VARCHAR(255) NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "description" VARCHAR(255) NOT NULL,
    "end" TIMESTAMP,
    "experiencePoints" INTEGER NOT NULL,
    "user" VARCHAR(255),
    "completed" BOOLEAN NOT NULL,
    "completionDate" TIMESTAMP,
    "groupID" BIGINT,
    "start" TIMESTAMP,
    PRIMARY KEY ("eventID", "user"),
    CONSTRAINT events_user_fkey FOREIGN KEY ("user")
    REFERENCES accounts("username")
    ON DELETE CASCADE
);

CREATE TABLE friends (
    "initiatingUser" VARCHAR(255) NOT NULL,
    "befriendedUser" VARCHAR(255) NOT NULL,
    PRIMARY KEY ("initiatingUser", "befriendedUser"),
    CONSTRAINT friends_initiatingUser_fkey FOREIGN KEY ("initiatingUser")
    REFERENCES accounts("username")
    ON DELETE CASCADE,
    CONSTRAINT friends_befriendedUser_fkey FOREIGN KEY ("befriendedUser")
    REFERENCES accounts("username")
    ON DELETE CASCADE
);