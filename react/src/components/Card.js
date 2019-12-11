import React from 'react';
import ReactCardFlip from 'react-card-flip';
import { message } from 'flwww';

const Card = ({ info, ws, gameId, currentPlayersTurn, cardId }) => {
  const [isFlipped, setIsFlipped] = React.useState(false);
  const getImage = id => {
    switch (id) {
      case 0:
        return (
          <img
            src={require('../assets/Card_visuals/Arcade_machine.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 2:
        return (
          <img
            src={require('../assets/Card_visuals/Cassette_tape.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 4:
        return (
          <img
            src={require('../assets/Card_visuals/Computer.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 6:
        return (
          <img
            src={require('../assets/Card_visuals/Floppy_disk.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 8:
        return (
          <img
            src={require('../assets/Card_visuals/Gameboy.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 10:
        return (
          <img
            src={require('../assets/Card_visuals/Headphone.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 12:
        return (
          <img
            src={require('../assets/Card_visuals/Keyboard.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 14:
        return (
          <img
            src={require('../assets/Card_visuals/Monitor.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 16:
        return (
          <img
            src={require('../assets/Card_visuals/Mouse.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 18:
        return (
          <img
            src={require('../assets/Card_visuals/NES.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 20:
        return (
          <img
            src={require('../assets/Card_visuals/Tamagotchi.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      case 22:
        return (
          <img
            src={require('../assets/Card_visuals/VHS.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
      default:
        return (
          <img
            src={require('../assets/Card_visuals/king_card.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        );
    }
  };

  React.useEffect(() => {
    getImage(info.cardId);
  }, [info.cardId]);

  const handleClick = e => {
    const x = info.x.toString();
    const y = info.y.toString();
    const userId = localStorage.getItem('id');
    const responseBody = gameId + ',' + userId + ',' + x + ',' + y;
    const flipCardInfo = {
      responseType: 'Flip Card',
      responseBody: responseBody
    };

    ws.current.send(JSON.stringify(flipCardInfo));
    setIsFlipped(!isFlipped);
    console.log('this is the card info: ');
    console.log(info);
  };

  return (
    <ReactCardFlip
      isFlipped={info.isRevealed}
      flipDirection='horizontal'
      style={{ height: '100px' }}
    >
      <div
        onClick={() => {
          if (
            currentPlayersTurn !== localStorage.getItem('id') ||
            info.isRevealed
          ) {
            message('It is not your turn', 'error');
          }
        }}
      >
        <button
          onClick={handleClick}
          disabled={
            currentPlayersTurn !== localStorage.getItem('id') || info.isRevealed
          }
        >
          <img
            src={require('../assets/Card_visuals/Gators.png')}
            alt='gator'
            style={{ height: '100px' }}
          ></img>
        </button>
      </div>

      <div
        onClick={() => {
          if (
            currentPlayersTurn !== localStorage.getItem('id') ||
            info.isRevealed
          ) {
            message('It is not your turn', 'error');
          }
        }}
      >
        <button
          onClick={handleClick}
          disabled={
            currentPlayersTurn !== localStorage.getItem('id') || info.isRevealed
          }
        >
          {getImage(info.cardId)}
        </button>
      </div>
    </ReactCardFlip>
  );
};

export default Card;
