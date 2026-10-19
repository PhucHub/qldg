import {
  pgTable,
  serial,
  text,
  varchar,
  integer,
  boolean,
  timestamp,
} from "drizzle-orm/pg-core";

export const campuses = pgTable("campuses", {
  id: serial("id").primaryKey(),
  code: varchar("code", { length: 20 }).notNull().unique(),
  name: text("name").notNull(),
  address: text("address"),
});

export const faculties = pgTable("faculties", {
  id: serial("id").primaryKey(),
  code: varchar("code", { length: 20 }).notNull().unique(),
  name: text("name").notNull(),
});

export const semesters = pgTable("semesters", {
  id: serial("id").primaryKey(),
  term: varchar("term", { length: 10 }).notNull(),
  year: integer("year").notNull(),
  ordinal: integer("ordinal").notNull(),
  startDate: varchar("start_date", { length: 20 }).notNull(),
  block1End: varchar("block1_end", { length: 20 }).notNull(),
  block2Start: varchar("block2_start", { length: 20 }).notNull(),
  endDate: varchar("end_date", { length: 20 }).notNull(),
});

export const users = pgTable("users", {
  id: serial("id").primaryKey(),
  account: varchar("account", { length: 50 }).notNull(),
  email: text("email").notNull(),
  fullName: text("full_name"),
  staffCode: varchar("staff_code", { length: 20 }),
  role: varchar("role", { length: 20 }).notNull(),
  campusId: integer("campus_id")
    .notNull()
    .references(() => campuses.id),
  facultyId: integer("faculty_id").references(() => faculties.id),
  active: boolean("active").notNull().default(true),
  headOffice: boolean("head_office").notNull().default(false),
});

export const campusFaculty = pgTable("campus_faculty", {
  id: serial("id").primaryKey(),
  campusId: integer("campus_id")
    .notNull()
    .references(() => campuses.id),
  facultyId: integer("faculty_id")
    .notNull()
    .references(() => faculties.id),
  active: boolean("is_active").notNull().default(true),
  groupKey: varchar("group_key", { length: 30 }),
  cnbmId: integer("cnbm_id").references(() => users.id),
  tmId: integer("tm_id").references(() => users.id),
});

export const subjects = pgTable("subjects", {
  id: serial("id").primaryKey(),
  code: varchar("code", { length: 20 }).notNull().unique(),
  name: text("name").notNull(),
  facultyId: integer("faculty_id")
    .notNull()
    .references(() => faculties.id),
  autoCreated: boolean("auto_created").notNull().default(false),
});

export const teachings = pgTable("subject_user", {
  id: serial("id").primaryKey(),
  classCode: varchar("class_code", { length: 30 }).notNull(),
  subjectId: integer("subject_id")
    .notNull()
    .references(() => subjects.id),
  semesterId: integer("semester_id")
    .notNull()
    .references(() => semesters.id),
  block: integer("block").notNull(),
  teacherId: integer("teacher_id")
    .notNull()
    .references(() => users.id),
  campusId: integer("campus_id")
    .notNull()
    .references(() => campuses.id),
});

export const calendars = pgTable("calendars", {
  id: serial("id").primaryKey(),
  teachingId: integer("teaching_id")
    .notNull()
    .references(() => teachings.id),
  teacherId: integer("teacher_id")
    .notNull()
    .references(() => users.id),
  date: varchar("date", { length: 20 }).notNull(),
  shift: integer("shift").notNull(),
  room: varchar("room", { length: 30 }).notNull(),
  sessionNo: integer("session_no"),
});

export const importJobs = pgTable("queue_tasks", {
  id: serial("id").primaryKey(),
  fileName: text("file_name"),
  storedPath: text("stored_path"),
  fileSize: integer("file_size").notNull().default(0),
  campusId: integer("campus_id")
    .notNull()
    .references(() => campuses.id),
  uploaderId: integer("uploader_id")
    .notNull()
    .references(() => users.id),
  status: varchar("status", { length: 20 }).notNull().default("PENDING"),
  totalRows: integer("total_rows").notNull().default(0),
  inserted: integer("inserted").notNull().default(0),
  updated: integer("updated").notNull().default(0),
  skipped: integer("skipped").notNull().default(0),
  errors: integer("errors").notNull().default(0),
  progress: integer("progress").notNull().default(0),
  message: varchar("message", { length: 500 }),
  createdAt: timestamp("created_at").defaultNow(),
  finishedAt: timestamp("finished_at"),
});

export const importRows = pgTable("import_rows", {
  id: serial("id").primaryKey(),
  jobId: integer("job_id")
    .notNull()
    .references(() => importJobs.id),
  rowNo: integer("row_no").notNull(),
  classCode: text("class_code"),
  subjectCode: text("subject_code"),
  teacherAccount: text("teacher_account"),
  sessionNo: integer("session_no"),
  result: varchar("result", { length: 20 }),
  oldValue: text("old_value"),
  newValue: text("new_value"),
  message: varchar("message", { length: 500 }),
});

export const newSubjectAlerts = pgTable("new_subject_alerts", {
  id: serial("id").primaryKey(),
  jobId: integer("job_id")
    .notNull()
    .references(() => importJobs.id),
  subjectId: integer("subject_id")
    .notNull()
    .references(() => subjects.id),
  kind: varchar("kind", { length: 20 }),
  baseCode: text("base_code"),
  classCount: integer("class_count").notNull().default(0),
});

export const inspections = pgTable("inspections", {
  id: serial("id").primaryKey(),
  code: varchar("code", { length: 60 }).notNull().unique(),
  campusId: integer("campus_id")
    .notNull()
    .references(() => campuses.id),
  facultyId: integer("faculty_id")
    .notNull()
    .references(() => faculties.id),
  semesterId: integer("semester_id")
    .notNull()
    .references(() => semesters.id),
  block: integer("block").notNull(),
  creatorId: integer("creator_id")
    .notNull()
    .references(() => users.id),
  status: varchar("status", { length: 25 }).notNull().default("DRAFT"),
  unscheduled: boolean("unscheduled").notNull().default(false),
  lastNote: varchar("last_note", { length: 1000 }),
  createdAt: timestamp("created_at").defaultNow(),
  submittedAt: timestamp("submitted_at"),
});

export const inspectionDetails = pgTable("inspection_details", {
  id: serial("id").primaryKey(),
  inspectionId: integer("inspection_id")
    .notNull()
    .references(() => inspections.id),
  teachingId: integer("teaching_id")
    .notNull()
    .references(() => teachings.id),
  plannedDate: varchar("planned_date", { length: 20 }),
  shift: integer("shift"),
  inspector1Id: integer("inspector1_id").references(() => users.id),
  inspector2Id: integer("inspector2_id").references(() => users.id),
  inspector3Id: integer("inspector3_id").references(() => users.id),
  reason: varchar("reason", { length: 500 }),
});

export const inspectionHistories = pgTable("inspection_histories", {
  id: serial("id").primaryKey(),
  inspectionId: integer("inspection_id")
    .notNull()
    .references(() => inspections.id),
  action: varchar("action", { length: 25 }),
  note: varchar("note", { length: 1000 }),
  actorId: integer("actor_id")
    .notNull()
    .references(() => users.id),
  createdAt: timestamp("created_at").defaultNow(),
});

export const notifications = pgTable("notifications", {
  id: serial("id").primaryKey(),
  userId: integer("user_id")
    .notNull()
    .references(() => users.id),
  message: varchar("message", { length: 500 }),
  link: text("link"),
  read: boolean("is_read").notNull().default(false),
  createdAt: timestamp("created_at").defaultNow(),
});
