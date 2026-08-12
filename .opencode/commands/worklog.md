# /worklog — Add a worklog entry for this session

When the user runs `/worklog`, do the following:

1. Read the current `worklog.md` to get the next session number
2. Create a new file `worklog/YYYY-MM-DD-session-N.md` with:
   - Date and session number
   - Summary of what was done in this session (based on git log and file changes)
   - Test counts per module
   - List of commits made
   - Issues encountered and how they were resolved
   - Files changed
3. Update `worklog.md` index to include the new session
4. Confirm to the user that the worklog entry was created

Use the format from `worklog/2026-08-12-session-1.md` as a template.
