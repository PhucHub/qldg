CREATE TABLE "calendars" (
	"id" serial PRIMARY KEY,
	"teaching_id" integer NOT NULL,
	"teacher_id" integer NOT NULL,
	"date" varchar(20) NOT NULL,
	"shift" integer NOT NULL,
	"room" varchar(30) NOT NULL,
	"session_no" integer
);
--> statement-breakpoint
CREATE TABLE "campus_faculty" (
	"id" serial PRIMARY KEY,
	"campus_id" integer NOT NULL,
	"faculty_id" integer NOT NULL,
	"is_active" boolean DEFAULT true NOT NULL,
	"group_key" varchar(30),
	"cnbm_id" integer,
	"tm_id" integer
);
--> statement-breakpoint
CREATE TABLE "campuses" (
	"id" serial PRIMARY KEY,
	"code" varchar(20) NOT NULL UNIQUE,
	"name" text NOT NULL,
	"address" text
);
--> statement-breakpoint
CREATE TABLE "faculties" (
	"id" serial PRIMARY KEY,
	"code" varchar(20) NOT NULL UNIQUE,
	"name" text NOT NULL
);
--> statement-breakpoint
CREATE TABLE "queue_tasks" (
	"id" serial PRIMARY KEY,
	"file_name" text,
	"stored_path" text,
	"file_size" integer DEFAULT 0 NOT NULL,
	"campus_id" integer NOT NULL,
	"uploader_id" integer NOT NULL,
	"status" varchar(20) DEFAULT 'PENDING' NOT NULL,
	"total_rows" integer DEFAULT 0 NOT NULL,
	"inserted" integer DEFAULT 0 NOT NULL,
	"updated" integer DEFAULT 0 NOT NULL,
	"skipped" integer DEFAULT 0 NOT NULL,
	"errors" integer DEFAULT 0 NOT NULL,
	"progress" integer DEFAULT 0 NOT NULL,
	"message" varchar(500),
	"created_at" timestamp DEFAULT now(),
	"finished_at" timestamp
);
--> statement-breakpoint
CREATE TABLE "import_rows" (
	"id" serial PRIMARY KEY,
	"job_id" integer NOT NULL,
	"row_no" integer NOT NULL,
	"class_code" text,
	"subject_code" text,
	"teacher_account" text,
	"session_no" integer,
	"result" varchar(20),
	"old_value" text,
	"new_value" text,
	"message" varchar(500)
);
--> statement-breakpoint
CREATE TABLE "inspection_details" (
	"id" serial PRIMARY KEY,
	"inspection_id" integer NOT NULL,
	"teaching_id" integer NOT NULL,
	"planned_date" varchar(20),
	"shift" integer,
	"inspector1_id" integer,
	"inspector2_id" integer,
	"inspector3_id" integer,
	"reason" varchar(500)
);
--> statement-breakpoint
CREATE TABLE "inspection_histories" (
	"id" serial PRIMARY KEY,
	"inspection_id" integer NOT NULL,
	"action" varchar(25),
	"note" varchar(1000),
	"actor_id" integer NOT NULL,
	"created_at" timestamp DEFAULT now()
);
--> statement-breakpoint
CREATE TABLE "inspections" (
	"id" serial PRIMARY KEY,
	"code" varchar(60) NOT NULL UNIQUE,
	"campus_id" integer NOT NULL,
	"faculty_id" integer NOT NULL,
	"semester_id" integer NOT NULL,
	"block" integer NOT NULL,
	"creator_id" integer NOT NULL,
	"status" varchar(25) DEFAULT 'DRAFT' NOT NULL,
	"unscheduled" boolean DEFAULT false NOT NULL,
	"last_note" varchar(1000),
	"created_at" timestamp DEFAULT now(),
	"submitted_at" timestamp
);
--> statement-breakpoint
CREATE TABLE "new_subject_alerts" (
	"id" serial PRIMARY KEY,
	"job_id" integer NOT NULL,
	"subject_id" integer NOT NULL,
	"kind" varchar(20),
	"base_code" text,
	"class_count" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE "notifications" (
	"id" serial PRIMARY KEY,
	"user_id" integer NOT NULL,
	"message" varchar(500),
	"link" text,
	"is_read" boolean DEFAULT false NOT NULL,
	"created_at" timestamp DEFAULT now()
);
--> statement-breakpoint
CREATE TABLE "semesters" (
	"id" serial PRIMARY KEY,
	"term" varchar(10) NOT NULL,
	"year" integer NOT NULL,
	"ordinal" integer NOT NULL,
	"start_date" varchar(20) NOT NULL,
	"block1_end" varchar(20) NOT NULL,
	"block2_start" varchar(20) NOT NULL,
	"end_date" varchar(20) NOT NULL
);
--> statement-breakpoint
CREATE TABLE "subjects" (
	"id" serial PRIMARY KEY,
	"code" varchar(20) NOT NULL UNIQUE,
	"name" text NOT NULL,
	"faculty_id" integer NOT NULL,
	"auto_created" boolean DEFAULT false NOT NULL
);
--> statement-breakpoint
CREATE TABLE "subject_user" (
	"id" serial PRIMARY KEY,
	"class_code" varchar(30) NOT NULL,
	"subject_id" integer NOT NULL,
	"semester_id" integer NOT NULL,
	"block" integer NOT NULL,
	"teacher_id" integer NOT NULL,
	"campus_id" integer NOT NULL
);
--> statement-breakpoint
CREATE TABLE "users" (
	"id" serial PRIMARY KEY,
	"account" varchar(50) NOT NULL,
	"email" text NOT NULL,
	"full_name" text,
	"staff_code" varchar(20),
	"role" varchar(20) NOT NULL,
	"campus_id" integer NOT NULL,
	"faculty_id" integer,
	"active" boolean DEFAULT true NOT NULL,
	"head_office" boolean DEFAULT false NOT NULL
);
--> statement-breakpoint
ALTER TABLE "calendars" ADD CONSTRAINT "calendars_teaching_id_subject_user_id_fkey" FOREIGN KEY ("teaching_id") REFERENCES "subject_user"("id");--> statement-breakpoint
ALTER TABLE "calendars" ADD CONSTRAINT "calendars_teacher_id_users_id_fkey" FOREIGN KEY ("teacher_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "campus_faculty" ADD CONSTRAINT "campus_faculty_campus_id_campuses_id_fkey" FOREIGN KEY ("campus_id") REFERENCES "campuses"("id");--> statement-breakpoint
ALTER TABLE "campus_faculty" ADD CONSTRAINT "campus_faculty_faculty_id_faculties_id_fkey" FOREIGN KEY ("faculty_id") REFERENCES "faculties"("id");--> statement-breakpoint
ALTER TABLE "campus_faculty" ADD CONSTRAINT "campus_faculty_cnbm_id_users_id_fkey" FOREIGN KEY ("cnbm_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "campus_faculty" ADD CONSTRAINT "campus_faculty_tm_id_users_id_fkey" FOREIGN KEY ("tm_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "queue_tasks" ADD CONSTRAINT "queue_tasks_campus_id_campuses_id_fkey" FOREIGN KEY ("campus_id") REFERENCES "campuses"("id");--> statement-breakpoint
ALTER TABLE "queue_tasks" ADD CONSTRAINT "queue_tasks_uploader_id_users_id_fkey" FOREIGN KEY ("uploader_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "import_rows" ADD CONSTRAINT "import_rows_job_id_queue_tasks_id_fkey" FOREIGN KEY ("job_id") REFERENCES "queue_tasks"("id");--> statement-breakpoint
ALTER TABLE "inspection_details" ADD CONSTRAINT "inspection_details_inspection_id_inspections_id_fkey" FOREIGN KEY ("inspection_id") REFERENCES "inspections"("id");--> statement-breakpoint
ALTER TABLE "inspection_details" ADD CONSTRAINT "inspection_details_teaching_id_subject_user_id_fkey" FOREIGN KEY ("teaching_id") REFERENCES "subject_user"("id");--> statement-breakpoint
ALTER TABLE "inspection_details" ADD CONSTRAINT "inspection_details_inspector1_id_users_id_fkey" FOREIGN KEY ("inspector1_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "inspection_details" ADD CONSTRAINT "inspection_details_inspector2_id_users_id_fkey" FOREIGN KEY ("inspector2_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "inspection_details" ADD CONSTRAINT "inspection_details_inspector3_id_users_id_fkey" FOREIGN KEY ("inspector3_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "inspection_histories" ADD CONSTRAINT "inspection_histories_inspection_id_inspections_id_fkey" FOREIGN KEY ("inspection_id") REFERENCES "inspections"("id");--> statement-breakpoint
ALTER TABLE "inspection_histories" ADD CONSTRAINT "inspection_histories_actor_id_users_id_fkey" FOREIGN KEY ("actor_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "inspections" ADD CONSTRAINT "inspections_campus_id_campuses_id_fkey" FOREIGN KEY ("campus_id") REFERENCES "campuses"("id");--> statement-breakpoint
ALTER TABLE "inspections" ADD CONSTRAINT "inspections_faculty_id_faculties_id_fkey" FOREIGN KEY ("faculty_id") REFERENCES "faculties"("id");--> statement-breakpoint
ALTER TABLE "inspections" ADD CONSTRAINT "inspections_semester_id_semesters_id_fkey" FOREIGN KEY ("semester_id") REFERENCES "semesters"("id");--> statement-breakpoint
ALTER TABLE "inspections" ADD CONSTRAINT "inspections_creator_id_users_id_fkey" FOREIGN KEY ("creator_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "new_subject_alerts" ADD CONSTRAINT "new_subject_alerts_job_id_queue_tasks_id_fkey" FOREIGN KEY ("job_id") REFERENCES "queue_tasks"("id");--> statement-breakpoint
ALTER TABLE "new_subject_alerts" ADD CONSTRAINT "new_subject_alerts_subject_id_subjects_id_fkey" FOREIGN KEY ("subject_id") REFERENCES "subjects"("id");--> statement-breakpoint
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_user_id_users_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "subjects" ADD CONSTRAINT "subjects_faculty_id_faculties_id_fkey" FOREIGN KEY ("faculty_id") REFERENCES "faculties"("id");--> statement-breakpoint
ALTER TABLE "subject_user" ADD CONSTRAINT "subject_user_subject_id_subjects_id_fkey" FOREIGN KEY ("subject_id") REFERENCES "subjects"("id");--> statement-breakpoint
ALTER TABLE "subject_user" ADD CONSTRAINT "subject_user_semester_id_semesters_id_fkey" FOREIGN KEY ("semester_id") REFERENCES "semesters"("id");--> statement-breakpoint
ALTER TABLE "subject_user" ADD CONSTRAINT "subject_user_teacher_id_users_id_fkey" FOREIGN KEY ("teacher_id") REFERENCES "users"("id");--> statement-breakpoint
ALTER TABLE "subject_user" ADD CONSTRAINT "subject_user_campus_id_campuses_id_fkey" FOREIGN KEY ("campus_id") REFERENCES "campuses"("id");--> statement-breakpoint
ALTER TABLE "users" ADD CONSTRAINT "users_campus_id_campuses_id_fkey" FOREIGN KEY ("campus_id") REFERENCES "campuses"("id");--> statement-breakpoint
ALTER TABLE "users" ADD CONSTRAINT "users_faculty_id_faculties_id_fkey" FOREIGN KEY ("faculty_id") REFERENCES "faculties"("id");