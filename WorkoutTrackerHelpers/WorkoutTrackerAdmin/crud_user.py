import firebase_admin
from firebase_admin import credentials, auth
import pandas as pd

# Initialize Firebase Admin SDK
cred = credentials.Certificate('C:\\Users\\ethan\\Documents\\Work\\Impt\\'
                               'workout-wrecker-firebase-adminsdk-mw4y2-179f69fccc.json')
firebase_admin.initialize_app(cred)

file_path = 'C:/Users/ethan/Documents/Work/workout_tracker_users.xlsx'


def read_emails_from_excel(file_path, sheet_name):
    try:
        df = pd.read_excel(file_path, sheet_name=sheet_name)
        print(df)
        # Ensure the column name matches exactly
        if 'Email' not in df.columns:
            print(f"File does not contain an 'Email' column in sheet '{sheet_name}'.")
            return []
        return df['Email'].dropna().tolist()
    except FileNotFoundError:
        print(f'File not found: {file_path}')
        return []
    except Exception as e:
        print(f'Error reading file: {e}')
        return []


def confirm_deletion():
    confirmation = input("Type 'yes delete users' to confirm deletion\n::")
    return confirmation.lower() == "yes delete users"


def delete_users_from_list(file_path):
    if not confirm_deletion():
        print("Deletion cancelled.")
        return

    emails = read_emails_from_excel(file_path, "delete")
    if not emails:
        print("No emails found in the file.")
        return

    for email in emails:
        try:
            user = auth.get_user_by_email(email)
            auth.delete_user(user.uid)
            print(f'Successfully deleted user with email: {email}')
        except firebase_admin.auth.UserNotFoundError:
            print(f'User with email {email} not found')
        except Exception as e:
            print(f'Error deleting user with email: {email}', e)


def create_users_from_list(file_path):
    emails = read_emails_from_excel(file_path, "create")
    if not emails:
        print("No emails found in the file.")
        return

    for email in emails:
        try:
            user = auth.create_user(email=email)
            print(f'Successfully created user with email: {email}')
        except Exception as e:
            print(f'Error creating user with email: {email}', e)


choice = str(input("Create or Delete: "))
if choice == "Create":
    create_users_from_list(file_path)
elif choice == "Delete":
    delete_users_from_list(file_path)
