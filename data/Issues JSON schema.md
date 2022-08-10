# Issues JSON file

## Versions

A version must be specified as the top-level property, and
can have multiple version numbers. Valid version numbers
would include "1.19" (accounts for 1.19+) and "1.19, 1.18"
(accounts for 1.19+ AND 1.18+).

## Per-version

Each version is an array, which holds multiple issues. These
issues contain a title, solution, severity and a (list of)
causes. A summary of the purpose of each property is
provided below:

- **title:** The title/name of the issue which is shown to the user
 when their issue has been fully processed.
- **solution:** The full solution to the issue the user is having,
 this should link to some form of wiki/article if the solution is
 considered to long for a Discord embed.
- **severity:** The severity of the issue, this is used to determine
 the colour of the embed and how much it affects the user.
- **cause(s):** The list of causes for the issue, this is used when
 processing items for a potential issue.

### Severity

There are different types of bug/issue severities which are
more/less detrimental to the user when encountered. We can
find these issues and provide solutions to the user based
on the problem's severity. Valid severities are: `CRITICAL`,
`MAJOR`, `MINOR`, `TRIVIAL`.

### Causes

The causes property is an array of JSON objects which hold a search
method for the cause and the line describing the cause itself. Valid
search methods ("method") are: `CONTAINS`, `REGEX`.
