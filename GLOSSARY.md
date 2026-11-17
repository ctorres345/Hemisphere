# Hemisphere

An Android file manager with one or two panes, built for touch and gamepads.

## Layout

**Pane**:
One file browser showing one location. Single layout shows one pane; Dual shows two side by side.
_Avoid_: panel, column, window

**Active pane**:
The pane that receives input and actions. Marked with an accent bar.
_Avoid_: focused pane, current pane

**Layout mode**:
Auto, Single or Dual.
_Avoid_: view mode (that word is for List, Grid, Gallery)

**View mode**:
How a pane shows a folder: List, Grid or Gallery.
_Avoid_: layout, display mode

**Places**:
The list of volumes and shortcuts, shown as a drawer on a phone and a rail in Dual.
_Avoid_: sidebar, locations

## Storage

**Volume**:
One storage root: internal storage, an SD card, a USB OTG drive, or later a network share.
_Avoid_: drive, disk, storage, mount

**Entry**:
A file or folder inside a volume.
_Avoid_: item (outside jobs), node, document

## Jobs

**Job**:
One user action on files that runs in the queue, such as copying three files to a folder.
_Avoid_: task, operation, transfer

**Job queue**:
The ordered list of jobs, with their progress.
_Avoid_: operations queue, transfers, background tasks

**Conflict**:
A job wants to write an entry whose name already exists at the destination.
_Avoid_: collision, duplicate

**Clipboard tray**:
The bar at the bottom in Single layout that holds copied or cut entries until they are pasted.
_Avoid_: clipboard bar, paste bar

**Trash**:
The per-volume holding place for deleted entries, kept 30 days by default.
_Avoid_: recycle bin, bin

## Input

**Input mode**:
Touch or gamepad, set by the last input the user used.
_Avoid_: control scheme

**Focus ring**:
The visible outline on the element that gamepad input will act on.
_Avoid_: highlight, cursor

**Mark**:
To add an entry to the selection with the Ⓨ button.
_Avoid_: select (Select is a physical button)
