# import firebase_admin
# from firebase_admin import credentials, storage
#
# # Initialize the app with a service account, granting admin privileges
# cred = credentials.Certificate('C:\\Users\\ethan\\Downloads\\workout-wrecker-firebase-adminsdk-mw4y2-179f69fccc.json')
# firebase_admin.initialize_app(cred, {
#     'storageBucket': 'workout_wrecker.appspot.com'
# })
#
# bucket = storage.bucket()
#
# def delete_all_files():
#     try:
#         blobs = bucket.list_blobs()
#         for blob in blobs:
#             print(f'Deleting file: {blob.name}')
#             blob.delete()
#         print('All files deleted successfully.')
#     except Exception as e:
#         print(f'Error deleting files: {e}')
#
# if __name__ == '__main__':
#     delete_all_files()
