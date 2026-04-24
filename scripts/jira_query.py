#!/usr/bin/env python3
"""
Live Jira query helper for eGalvanic Z Platform (project key ZP).

Reads credentials in this priority order:
  1. Env vars:  JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN
  2. File:      ~/.jira_token   (plain text, one line = token; assumes JIRA_EMAIL
                                 fallback from $USER_EMAIL or default egalvanic.com)
  3. File:      ~/.jira_config  (key=value lines:
                                   url=https://egalvanic.atlassian.net
                                   email=abhiyant.singh@egalvanic.com
                                   token=<token>)

Supports read-only queries:
  - find <keyword>                 JQL search by summary contains
  - open-bugs [--web] [--limit N]  All open Bugs (optionally web-only)
  - ticket <key>                   Full details for one ticket
  - whoami                         Confirm auth works

Never writes/transitions tickets. That still requires explicit user permission
per feedback_never_modify_jira.md.

Usage:
  python3 scripts/jira_query.py whoami
  python3 scripts/jira_query.py find "issue class mandatory"
  python3 scripts/jira_query.py open-bugs --web --limit 20
  python3 scripts/jira_query.py ticket ZP-1319
"""
import os
import sys
import json
import base64
import urllib.request
import urllib.parse
import urllib.error
import argparse
import pathlib


def load_credentials():
    url = os.environ.get('JIRA_URL')
    email = os.environ.get('JIRA_EMAIL')
    token = os.environ.get('JIRA_API_TOKEN')

    config_path = pathlib.Path.home() / '.jira_config'
    token_path = pathlib.Path.home() / '.jira_token'

    if config_path.exists() and (not url or not email or not token):
        for line in config_path.read_text().splitlines():
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' not in line:
                continue
            k, v = line.split('=', 1)
            k, v = k.strip(), v.strip()
            if k == 'url' and not url:
                url = v
            elif k == 'email' and not email:
                email = v
            elif k == 'token' and not token:
                token = v

    if not token and token_path.exists():
        token = token_path.read_text().strip()

    # Sensible defaults inferred from codebase
    if not url:
        url = 'https://egalvanic.atlassian.net'
    if not email:
        email = 'abhiyant.singh@egalvanic.com'

    if not token:
        print('ERROR: No Jira API token found.', file=sys.stderr)
        print('  Set env var JIRA_API_TOKEN, OR', file=sys.stderr)
        print('  Write token to ~/.jira_token (chmod 600), OR', file=sys.stderr)
        print('  Write ~/.jira_config with url= / email= / token= lines', file=sys.stderr)
        print('Create a token at: https://id.atlassian.com/manage-profile/security/api-tokens',
              file=sys.stderr)
        sys.exit(2)

    return url.rstrip('/'), email, token


def api_get(base_url, email, token, path, params=None):
    auth = base64.b64encode(f'{email}:{token}'.encode()).decode()
    full = f'{base_url}{path}'
    if params:
        full += '?' + urllib.parse.urlencode(params)
    req = urllib.request.Request(full, headers={
        'Authorization': f'Basic {auth}',
        'Accept': 'application/json',
        'User-Agent': 'claude-code-jira-helper/1.0',
    })
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors='replace')[:500]
        print(f'HTTP {e.code} {e.reason}: {full}', file=sys.stderr)
        print(body, file=sys.stderr)
        if e.code == 401:
            print('\n→ 401 Unauthorized: token is wrong, expired, or email mismatch.', file=sys.stderr)
            print('  Regenerate at https://id.atlassian.com/manage-profile/security/api-tokens',
                  file=sys.stderr)
        sys.exit(3)
    except urllib.error.URLError as e:
        print(f'Network error hitting {full}: {e.reason}', file=sys.stderr)
        sys.exit(4)


def cmd_whoami(base_url, email, token, _):
    data = api_get(base_url, email, token, '/rest/api/3/myself')
    print(f'Authenticated as: {data.get("displayName", "?")} <{data.get("emailAddress", "?")}>')
    print(f'Account ID:       {data.get("accountId", "?")}')
    print(f'Jira URL:         {base_url}')
    print(f'Timezone:         {data.get("timeZone", "?")}')
    return 0


def run_jql(base_url, email, token, jql, limit, fields):
    params = {
        'jql': jql,
        'maxResults': str(limit),
        'fields': ','.join(fields),
    }
    return api_get(base_url, email, token, '/rest/api/3/search', params)


def format_row(issue):
    f = issue.get('fields', {})
    key = issue.get('key', '?')
    summary = (f.get('summary') or '')[:70]
    status = (f.get('status') or {}).get('name', '?')
    priority = (f.get('priority') or {}).get('name', '?')
    issue_type = (f.get('issuetype') or {}).get('name', '?')
    return f'  {key:<10} [{status:<18}] [{priority:<8}] ({issue_type:<8}) {summary}'


def cmd_find(base_url, email, token, args):
    keyword = args.keyword.replace('"', '\\"')
    jql = f'project = ZP AND summary ~ "{keyword}" ORDER BY updated DESC'
    fields = ['summary', 'status', 'priority', 'issuetype', 'updated']
    data = run_jql(base_url, email, token, jql, args.limit, fields)
    issues = data.get('issues', [])
    print(f'JQL: {jql}')
    print(f'Total: {data.get("total", 0)} (showing {len(issues)})')
    for i in issues:
        print(format_row(i))
    return 0


def cmd_open_bugs(base_url, email, token, args):
    parts = ['project = ZP', 'issuetype = Bug', 'statusCategory != Done']
    if args.web:
        parts.append('(summary ~ "web" OR summary ~ "frontend" OR summary ~ "FE" OR summary ~ "browser")')
    jql = ' AND '.join(parts) + ' ORDER BY priority DESC, updated DESC'
    fields = ['summary', 'status', 'priority', 'issuetype', 'updated']
    data = run_jql(base_url, email, token, jql, args.limit, fields)
    issues = data.get('issues', [])
    print(f'JQL: {jql}')
    print(f'Total: {data.get("total", 0)} open Bug(s) (showing {len(issues)})')
    for i in issues:
        print(format_row(i))
    return 0


def cmd_ticket(base_url, email, token, args):
    key = args.key
    data = api_get(base_url, email, token, f'/rest/api/3/issue/{key}')
    f = data.get('fields', {})
    print(f'Key:      {data.get("key")}')
    print(f'Summary:  {f.get("summary", "")}')
    print(f'Type:     {(f.get("issuetype") or {}).get("name")}')
    print(f'Status:   {(f.get("status") or {}).get("name")}')
    print(f'Priority: {(f.get("priority") or {}).get("name")}')
    print(f'Assignee: {(f.get("assignee") or {}).get("displayName", "Unassigned")}')
    print(f'Reporter: {(f.get("reporter") or {}).get("displayName", "?")}')
    print(f'Created:  {f.get("created", "?")[:10]}')
    print(f'Updated:  {f.get("updated", "?")[:10]}')
    desc = f.get('description')
    if isinstance(desc, dict):
        # ADF format - flatten to plain text
        parts = []
        for block in desc.get('content', []):
            for inner in block.get('content', []) or []:
                if inner.get('type') == 'text':
                    parts.append(inner.get('text', ''))
            parts.append('\n')
        desc = ''.join(parts).strip()
    print('\n--- Description ---')
    print(desc or '(none)')
    return 0


def cmd_dry_run(base_url, email, token, _):
    # No API call; just prove config loading works.
    print('Dry-run — config loaded:')
    print(f'  URL:   {base_url}')
    print(f'  Email: {email}')
    print(f'  Token: {"*" * 8}{token[-4:] if len(token) >= 4 else "???"} (length {len(token)})')
    print('Connection NOT tested. Run `whoami` to test live auth.')
    return 0


def main():
    parser = argparse.ArgumentParser(description='Read-only Jira query helper for project ZP')
    sub = parser.add_subparsers(dest='cmd', required=True)

    sub.add_parser('whoami', help='Confirm credentials work')
    sub.add_parser('dry-run', help='Confirm config loading without hitting API')

    p_find = sub.add_parser('find', help='Search tickets by summary keyword')
    p_find.add_argument('keyword')
    p_find.add_argument('--limit', type=int, default=20)

    p_bugs = sub.add_parser('open-bugs', help='List open Bug tickets in ZP')
    p_bugs.add_argument('--web', action='store_true', help='Filter to web/FE/browser/frontend')
    p_bugs.add_argument('--limit', type=int, default=30)

    p_ticket = sub.add_parser('ticket', help='Full details for one ticket')
    p_ticket.add_argument('key')

    args = parser.parse_args()
    url, email, token = load_credentials()

    handlers = {
        'whoami': cmd_whoami,
        'dry-run': cmd_dry_run,
        'find': cmd_find,
        'open-bugs': cmd_open_bugs,
        'ticket': cmd_ticket,
    }
    return handlers[args.cmd](url, email, token, args)


if __name__ == '__main__':
    sys.exit(main())
