# FixIt - Equipment Fault Management

## Project Overview
FixIt is a mobile application developed in Kotlin using Android Studio to manage equipment faults in an automotive parts factory. It streamlines fault registration, tracking, and resolution, improving operational efficiency and communication among Operators, Technicians, and Managers.

## Context
The application targets an automotive parts factory, managing faults in equipment such as CNC machines, conveyors, computers, and generators. It supports three user roles:
- **Operators**: Report faults from the factory floor.
- **Technicians**: Resolve assigned faults.
- **Managers**: Monitor operations, assign tasks, and analyze reports.

## Main Features
- Intro sliders for onboarding.
- Secure authentication with role-based access (Operator, Technician, Manager).
- Detailed fault registration (description, photo, urgency, location).
- Real-time fault monitoring via dashboards.
- Automatic and manual task assignment.
- In-app communication for fault-related messages.
- Push notifications for status updates.
- Reports and statistics for Managers (e.g., average resolution time).
- Offline mode with API synchronization.
- Support for Portuguese and English, portrait and landscape orientations.

## Repository Structure
- `/docs`: Documentation, including requirements and reports.
- `/src`: Source code.
- `/design`: Mockups, logo, and graphical resources.
- `/db`: Database schema and diagrams.
- `/releases`: Final signed APK.

## Deliverables (First Submission)
- **Requirements**: Software requirements (functional and non-functional) in `/docs/requirements.pdf`.
- **Project Management**: Trello board at [Trello](https://trello.com/invite/b/67ed571e86d233c016ce86b2/ATTI116a55377e6c4ee982a4ad98229fb697EC8902DF/fixit-gestao-do-projeto).
- **Design**: Mockups (PT/EN, portrait/landscape) in `/design/mockups/` and logo in `/design/fixit_logo.png`.
- **Database**: Schema in `/db/database_schema.md` and diagram in `/db/database_diagram.png`.

## Setup Instructions
1. Clone the repository:  
   `git clone https://github.com/brunoferreira17/FixIt-TP-Mobile.git`
2. Open the project in Android Studio.
3. Create a `local.properties` file in the root with the following (see `local.properties.example`):
   ```
   sdk.dir=C:\Users\YOUR_USER\AppData\Local\Android\Sdk
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_KEY=your-secret-api-key
   ```
4. Build and run the project.

## APK Download

The final signed APK is available in the `releases/` folder:

👉 [`app-release.apk`](./releases/app-release.apk)

To install on an Android device:
1. Transfer the file to your phone
2. Enable installation from unknown sources
3. Open the APK and install

## Testing

All features were tested manually across multiple scenarios and devices. See `TESTING.md` for a full list of tested flows and expected behaviors.

## Current Status
- Full implementation completed.
- Git repository active and updated.
- Trello used for task tracking.
- Functional tests completed and documented.
- Signed APK generated.
- All requirements from first and second submissions completed.

## Team
- Bruno Ferreira
- Diogo Perreira
