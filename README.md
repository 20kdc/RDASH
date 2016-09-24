#Resource Distribution And Synchronization Helper

I couldn't find a way to specify 'files' using the letter 'D'.

## Requirements for use

1. That the "server" folder is a mountpoint, or a symlink, or you copy everything from the medium to it and back.
2. That all clients have enough space for all the data in the system.
3. That the medium is shared (SFTP) or essentially shared (drive you carry).
4. That the medium can hold all the data you want to transfer at once.
5. *That you trust everybody with access to the medium, since not much verification is performed.*

## Reasons for writing

1. Portable storage is not as cheap as local storage, so file-level delta synchronization is needed.
2. The internet is slow, and you have to move between places anyway to cause the data to be transferred.
3. You do not tend to switch between computers every 5 seconds, requiring active sync.
4. This matches my use case exactly.
5. Using folders provides backend flexibility.

## Usage

1. Do any server-symlinking.
2. Make sure the "server" folder contains a "index" folder.
3. Create a folder for your computer's name.
4. `java -jar build/libs/Sync2-0.1.jar Standard`, and type in (case-sensitive) the computer name.
5. Repeat 1, 3 and 4 on other computers to ensure every computer on the network has it's index.
6. Repeat 4 on a given computer to synchronize.
7. Never run the sync on two computers at the same time.
8. Do not abort a sync while the index is being uploaded. (At other times, it *should* be safe.)

## In case of failure

If anything goes wrong, wipe the indexes, and re-run the synchronization program on each computer.
This could lead to partial-downloaded files being chosen over full files, though, so make a backup.

## In case of success

Have fun!
