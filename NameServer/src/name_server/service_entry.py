from dataclasses import dataclass
from name_server.server_exceptions import InvalidServiceEntry

@dataclass
class ServiceEntry:
    qual: str # service qualifier 
    addr: str # service address

    def __post_init__(self):
        """
        Validate server addres after instance initialization.

        If the server address is not a string of type "host:port" with a valid port
        value an exception will be propagated.

        :raises InvalidServiceEntry: If address doesn't follow the expected format. 
        """
        if not self.qual: # don't accept empty string as qualifier
            raise InvalidServiceEntry('Qualifier cannot be an empty string')

        ServiceEntry.is_valid_addr(self.addr)
   
    @staticmethod
    def is_valid_addr(addr: str):
        """ 
        Validate given ``addr`` is in the format 'host:port' where "port" is a valid port number.

        :param addr: Address to be validated.
        :rtype addr: str.

        :raises InvalidServiceEntry: If address doesn't follow the expected format.
        """
        if not isinstance(addr, str):
            raise InvalidServiceEntry(f'Expected str argument for address, got {type(addr)}')

        split_addr = addr.split(":")
        if len(split_addr) != 2:
            raise InvalidServiceEntry(f'Wrong address format: {addr}')
        
        if not split_addr[1].isdigit():
            raise InvalidServiceEntry(f'Entry address port is not an integer: {split_addr[1]}')
        
        if not (0 < int(split_addr[1]) < 2**16):
            raise InvalidServiceEntry(f'Entry address port is not a valid port number: {split_addr[1]}')
        
