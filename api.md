# Media Service API

<details>
  <summary><strong>Table of Contents</strong></summary>

  * [Query](#query)
  * [Mutation](#mutation)
  * [Objects](#objects)
    * [MediaRecord](#mediarecord)
    * [MediaRecordProgressData](#mediarecordprogressdata)
    * [PaginationInfo](#paginationinfo)
  * [Inputs](#inputs)
    * [CreateMediaRecordInput](#createmediarecordinput)
    * [DateTimeFilter](#datetimefilter)
    * [IntFilter](#intfilter)
    * [Pagination](#pagination)
    * [StringFilter](#stringfilter)
    * [UpdateMediaRecordInput](#updatemediarecordinput)
  * [Enums](#enums)
    * [MediaType](#mediatype)
    * [SortDirection](#sortdirection)
  * [Scalars](#scalars)
    * [Boolean](#boolean)
    * [Date](#date)
    * [DateTime](#datetime)
    * [Int](#int)
    * [LocalTime](#localtime)
    * [String](#string)
    * [Time](#time)
    * [UUID](#uuid)
    * [Url](#url)

</details>

## Query
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>mediaRecordsByIds</strong></td>
<td valign="top">[<a href="#mediarecord">MediaRecord</a>!]!</td>
<td>


Returns the media records with the given IDs. Throws an error if a MediaRecord corresponding to a given ID
cannot be found.
🔒 If the mediaRecord is associated with coursed the user must be a member of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">ids</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>findMediaRecordsByIds</strong></td>
<td valign="top">[<a href="#mediarecord">MediaRecord</a>]!</td>
<td>


Like mediaRecordsByIds() returns the media records with the given IDs, but instead of throwing an error if an ID
cannot be found, it instead returns NULL for that media record.
🔒 If the mediaRecord is associated with coursed the user must be a member of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">ids</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>mediaRecords</strong> ⚠️</td>
<td valign="top">[<a href="#mediarecord">MediaRecord</a>!]!</td>
<td>


Returns all media records of the system.
🔒 The user must be a super-user, otherwise an exception is thrown.

<p>⚠️ <strong>DEPRECATED</strong></p>
<blockquote>

In production there should probably be no way to get all media records of the system.

</blockquote>
</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userMediaRecords</strong></td>
<td valign="top">[<a href="#mediarecord">MediaRecord</a>!]!</td>
<td>


Returns all media records which the current user created.
🔒 If the mediaRecord is associated with coursed the user must be a member of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>mediaRecordsByContentIds</strong></td>
<td valign="top">[[<a href="#mediarecord">MediaRecord</a>!]!]!</td>
<td>


Returns the media records associated the given content IDs as a list of lists where each sublist contains
the media records associated with the content ID at the same index in the input list
🔒 If the mediaRecord is associated with coursed the user must be a member of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">contentIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>mediaRecordsForCourses</strong></td>
<td valign="top">[[<a href="#mediarecord">MediaRecord</a>!]!]!</td>
<td>


Returns all media records for the given CourseIds
🔒 If the mediaRecord is associated with coursed the user must be a member of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">courseIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>mediaRecordsForUsers</strong></td>
<td valign="top">[[<a href="#mediarecord">MediaRecord</a>!]!]!</td>
<td>


Returns all media records which were created by the users.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
</tbody>
</table>

## Mutation
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>createMediaRecord</strong></td>
<td valign="top"><a href="#mediarecord">MediaRecord</a>!</td>
<td>


Creates a new media record
🔒 The user must have the "course-creator" role to perform this action.
🔒 If the mediaRecord is associated with courses the user must be an administrator of all courses or a super-user.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#createmediarecordinput">CreateMediaRecordInput</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>updateMediaRecord</strong></td>
<td valign="top"><a href="#mediarecord">MediaRecord</a>!</td>
<td>


Updates an existing media record with the given UUID
🔒 If the mediaRecord is associated with courses the user must be an administrator of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#updatemediarecordinput">UpdateMediaRecordInput</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>deleteMediaRecord</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Deletes the media record with the given UUID
🔒 If the mediaRecord is associated with courses the user must be an administrator of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">id</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>setLinkedMediaRecordsForContent</strong></td>
<td valign="top">[<a href="#mediarecord">MediaRecord</a>!]!</td>
<td>


For a given MediaContent, sets the linked media records of it to the ones with the given UUIDs.
This means that for the content, all already linked media records are removed and replaced by the given ones.
🔒 If the mediaRecord is associated with courses the user must be an administrator of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">contentId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">mediaRecordIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>logMediaRecordWorkedOn</strong></td>
<td valign="top"><a href="#mediarecord">MediaRecord</a>!</td>
<td>


    Logs that a media has been worked on by the current user.
    See https://meitrex.readthedocs.io/en/latest/dev-manuals/gamification/userProgress.html

    Possible side effects:
    When all media records of a content have been worked on by a user,
    a user-progress event is emitted for the content.
    🔒 If the mediaRecord is associated with courses the user must be a member of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">mediaRecordId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>setMediaRecordsForCourse</strong></td>
<td valign="top">[<a href="#mediarecord">MediaRecord</a>!]!</td>
<td>


Add the MediaRecords with the given UUIDS to the Course with the given UUID.
🔒 If the mediaRecord is associated with courses the user must be an administrator of at least one of the courses.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">courseId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">mediaRecordIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
</tbody>
</table>

## Objects

### MediaRecord

 schema file of the microservice
 defines data types, queries and mutations
 this can be done in a separate files as long as they are in this folder and
 end with .graphqls

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the media record

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>courseIds</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


Ids of the courses this MediaRecord is associated with

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Name of the media record

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>creatorId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


User ID of the creator of the media record.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>type</strong></td>
<td valign="top"><a href="#mediatype">MediaType</a>!</td>
<td>


Type of the media record

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contentIds</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


IDs of the MediaContents this media record is associated with

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>uploadUrl</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Temporary upload url for the media record

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>downloadUrl</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Temporary download url for the media record

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userProgressData</strong></td>
<td valign="top"><a href="#mediarecordprogressdata">MediaRecordProgressData</a>!</td>
<td>


The progress data of the given user for this medium.

</td>
</tr>
</tbody>
</table>

### MediaRecordProgressData

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>workedOn</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


Whether the medium has been worked on by the user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>dateWorkedOn</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


Date on which the medium was worked on by the user.
This is null if the medium has not been worked on by the user.

</td>
</tr>
</tbody>
</table>

### PaginationInfo


Return type for information about paginated results.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The current page number.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalElements</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of elements across all pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalPages</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>hasNext</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


Whether there is a next page.

</td>
</tr>
</tbody>
</table>

## Inputs

### CreateMediaRecordInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Name of the media record. Cannot be blank, maximum length 255 characters.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>type</strong></td>
<td valign="top"><a href="#mediatype">MediaType</a>!</td>
<td>


Type of the media record.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contentIds</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


IDs of the MediaContents this media record is associated with

</td>
</tr>
</tbody>
</table>

### DateTimeFilter


Filter for date values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>after</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates after the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>before</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates before the specified value.

</td>
</tr>
</tbody>
</table>

### IntFilter


Filter for integer values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


An integer value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>greaterThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values greater than to the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>lessThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values less than to the specified value.

</td>
</tr>
</tbody>
</table>

### Pagination


Specifies the page size and page number for paginated results.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The page number, starting at 0.
If not specified, the default value is 0.
For values greater than 0, the page size must be specified.
If this value is larger than the number of pages, an empty page is returned.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
</tbody>
</table>

### StringFilter


Filter for string values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contains</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value that must be contained in the field that is being filtered.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>ignoreCase</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


If true, the filter is case-insensitive.

</td>
</tr>
</tbody>
</table>

### UpdateMediaRecordInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the media record which should be updated

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


New name of the media record. Cannot be blank, maximum length 255 characters.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>type</strong></td>
<td valign="top"><a href="#mediatype">MediaType</a>!</td>
<td>


New type of the media record.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contentIds</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


IDs of the MediaContents this media record is associated with

</td>
</tr>
</tbody>
</table>

## Enums

### MediaType


The type of the media record

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>VIDEO</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>AUDIO</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>IMAGE</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>PRESENTATION</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>DOCUMENT</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>URL</strong></td>
<td></td>
</tr>
</tbody>
</table>

### SortDirection


Specifies the sort direction, either ascending or descending.

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>ASC</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>DESC</strong></td>
<td></td>
</tr>
</tbody>
</table>

## Scalars

### Boolean

Built-in Boolean

### Date

An RFC-3339 compliant Full Date Scalar

### DateTime

A slightly refined version of RFC-3339 compliant DateTime Scalar

### Int

Built-in Int

### LocalTime

24-hour clock time value string in the format `hh:mm:ss` or `hh:mm:ss.sss`.

### String

Built-in String

### Time

An RFC-3339 compliant Full Time Scalar

### UUID

A universally unique identifier compliant UUID Scalar

### Url

A Url scalar

