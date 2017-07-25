--- Status: 24.07.2017
--- Execute this in BigQuery

--- TODO: Check extraction of SO references (e.g. post 3758880)

--- Select all source code lines of text files that contain a link to Stack Overflow
SELECT
  id,
  size,
  REGEXP_REPLACE(
    REGEXP_EXTRACT(LOWER(line), r'(https?://stackoverflow\.com/[^\s)\.\"]*)'),
    r'(^https)',
    'http'
  ) as url,
  line
FROM (
  SELECT
    id,
    size,
    SPLIT(content, '\n') as line
  FROM [bigquery-public-data:github_repos.contents]
  WHERE
    binary = false
    AND content is not null 
)
WHERE REGEXP_MATCH(line, r'(?i:https?://stackoverflow\.com/[^\s)\.\"]*)');
=> so_references.matched_lines


--- Join with table files to get information about repos
SELECT
  files.id as file_id,
  repo_name,
  REGEXP_EXTRACT(ref, r'refs/heads/(.+)') as branch,
  path,
  size,
  url,
  line
FROM [soposthistory:so_references.matched_lines] as lines
INNER JOIN [bigquery-public-data:github_repos.files] as files
ON lines.id = files.id;
=> so_references.matched_files


--- Normalize the SO links to (http://stackoverflow.com/(a/q)/<id>)
SELECT
  file_id,
  repo_name,
  branch,
  path,
  size,
  CASE
    WHEN REGEXP_MATCH(url, r'(http:\/\/stackoverflow\.com\/(?:a|q)\/[\d]+)')
    THEN REGEXP_EXTRACT(url, r'http:\/\/stackoverflow\.com\/(?:a|q)\/([\d]+)')
    WHEN REGEXP_MATCH(url, r'https?:\/\/stackoverflow\.com\/questions\/[\d]+\/[^\s\/\#]+(?:\/|\#)([\d]+)')
    THEN CONCAT("http://stackoverflow.com/a/", REGEXP_EXTRACT(url, r'https?:\/\/stackoverflow\.com\/questions\/[\d]+\/[^\s\/\#]+(?:\/|\#)([\d]+)'))
    WHEN REGEXP_MATCH(url, r'(https?:\/\/stackoverflow\.com\/questions\/[\d]+)')
    THEN CONCAT("http://stackoverflow.com/q/", REGEXP_EXTRACT(url, r'https?:\/\/stackoverflow\.com\/questions\/([\d]+)'))
    ELSE url
  END as url,
  line
FROM [soposthistory:so_references.matched_files];
=> so_references.matched_files_normalized


--- Extract post id from links, set post type id, and extract file extension from path
SELECT
  file_id,
  repo_name,
  branch,
  path,
  LOWER(REGEXP_EXTRACT(path, r'(\.[^.]+$)')) as file_ext,
  size,
  REGEXP_EXTRACT(url, r'http:\/\/stackoverflow\.com\/(?:a|q)\/([\d]+)') as post_id,
  CASE
    WHEN REGEXP_MATCH(url, r'(http:\/\/stackoverflow\.com\/q\/[\d]+)')
    THEN 1
    WHEN REGEXP_MATCH(url, r'(http:\/\/stackoverflow\.com\/a\/[\d]+)')
    THEN 2
    ELSE NULL
  END as post_type_id,
  url,
  line
FROM [soposthistory:so_references.matched_files_normalized]
WHERE
  REGEXP_MATCH(url, r'(http:\/\/stackoverflow\.com\/(?:a|q)\/[\d]+)');
=> so_references.matched_files_aq


--- Use camel case for column names and remove line content for export into MySQL database
SELECT
  file_id as FileId,
  repo_name as RepoName,
  branch as Branch,
  path as Path,
  file_ext as FileExt,
  size as Size,
  post_id as PostId,
  post_type_id as PostTypeId,
  url as Url
FROM [soposthistory:so_references.matched_files_aq];
=> so_references.PostReferenceGH

  